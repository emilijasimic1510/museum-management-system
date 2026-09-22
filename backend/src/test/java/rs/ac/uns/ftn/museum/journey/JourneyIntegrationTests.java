package rs.ac.uns.ftn.museum.journey;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;
import rs.ac.uns.ftn.museum.journey.service.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@ActiveProfiles("test")
class JourneyIntegrationTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JourneyMaintenance maintenance;
    @MockitoBean GraphProjection graph;
    private record User(long id,String token) { @Override public String toString(){return "Test user";} }
    private record Fixture(long exhibition,long term,long first,long second,String firstQr,String secondQr) {}
    private JsonNode call(MockHttpServletRequestBuilder request,User user,Object body,int expected) throws Exception {
        if(user!=null)request.header("Authorization","Bearer "+user.token());
        if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String result=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return result.isBlank()?json.nullNode():json.readTree(result);
    }
    private User user(String... roles) throws Exception {
        String email=UUID.randomUUID()+"@example.test", password=UUID.randomUUID().toString();
        long id=call(post("/api/auth/register"),null,Map.of("email",email,"password",password,"firstName","Test","lastName","Journey","recommendationsConsent",false),201).get("id").asLong();
        for(String role:roles)jdbc.update("insert into nalog_uloga(nalog_id,uloga_id,datum_dodele) select ?,id,now() from uloga where naziv=? on conflict do nothing",id,role);
        return new User(id,call(post("/api/auth/login"),null,Map.of("email",email,"password",password),200).get("accessToken").asText());
    }
    private Fixture fixture(int capacity) {
        return new TransactionTemplate(transactions).execute(status->{
            long museum=jdbc.queryForObject("insert into muzej(naziv,adresa,aktivan) values ('Journey','Test',true) returning id",Long.class);
            long hall=jdbc.queryForObject("insert into sala(muzej_id,naziv) values (?,'Journey') returning id",Long.class,museum);
            long exhibition=jdbc.queryForObject("insert into izlozba(sala_id,naziv,datum_pocetka,datum_zavrsetka,status,online_vidljiva) values (?,'Journey',now()-interval '2 days',now()+interval '2 days','AKTIVNA',true) returning id",Long.class,hall);
            long term=jdbc.queryForObject("insert into termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status) values (?,now()-interval '1 hour',now()+interval '1 hour',?,'OTVOREN') returning id",Long.class,exhibition,capacity);
            jdbc.update("insert into cenovnik_izlozbe values (?,now()-interval '1 day','REDOVNA',null,12.50,null)",exhibition);
            long exhibit=jdbc.queryForObject("insert into eksponat(naziv,inventarski_broj,tip) values ('First',?,'Slika') returning id",Long.class,UUID.randomUUID().toString());
            String firstQr=UUID.randomUUID().toString(),secondQr=UUID.randomUUID().toString();
            long first=jdbc.queryForObject("insert into ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,qr_token,redosled_obilaska) values (?,?,?,1) returning id_ucesca",Long.class,exhibition,exhibit,firstQr);
            long other=jdbc.queryForObject("insert into eksponat(naziv,inventarski_broj,tip) values ('Second',?,'Slika') returning id",Long.class,UUID.randomUUID().toString());
            long second=jdbc.queryForObject("insert into ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,qr_token,redosled_obilaska) values (?,?,?,2) returning id_ucesca",Long.class,exhibition,other,secondQr);
            return new Fixture(exhibition,term,first,second,firstQr,secondQr);
        });
    }
    private Map<String,Object> purchaseBody(Fixture f,boolean decline) { return Map.of("termId",f.term(),"category","REDOVNA","paymentMethod","KARTICA","demoDecline",decline); }
    private JsonNode buy(Fixture f,User user) throws Exception { return call(post("/api/tickets"),user,purchaseBody(f,false),201); }
    private long enter(Fixture f,User cashier,JsonNode ticket) throws Exception {
        return call(post("/api/cashier/validate"),cashier,Map.of("qrToken",ticket.get("qrToken").asText(),"termId",f.term()),201).get("id").asLong();
    }
    @Test void allMigrationsAndThirtyOneMappingsValidate() {
        assertThat(jdbc.queryForList("select version from flyway_schema_history where success order by installed_rank",String.class)).containsExactly("1","2","3");
        assertThat(em.getMetamodel().getEntities()).hasSize(31);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name<>'flyway_schema_history'",Integer.class)).isEqualTo(31);
    }
    @Test void serverPriceSnapshotAndPrivateTicketHistory() throws Exception {
        Fixture f=fixture(3);User owner=user(),other=user();
        JsonNode ticket=buy(f,owner);long id=ticket.get("id").asLong();
        assertThat(ticket.get("paidPrice").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(ticket.get("qrToken").asText()).hasSize(43);
        jdbc.update("update cenovnik_izlozbe set cena=99 where izlozba_id=?",f.exhibition());
        assertThat(call(get("/api/tickets/"+id),owner,null,200).get("paidPrice").decimalValue()).isEqualByComparingTo("12.50");
        call(get("/api/tickets/"+id),other,null,403);
        call(get("/api/tickets/"+id+"/payments"),other,null,403);
        assertThat(call(get("/api/tickets/"+id+"/payments"),owner,null,200).get(0).get("status").asText()).isEqualTo("USPESNA");
        assertThat(call(get("/api/tickets/me"),owner,null,200).size()).isEqualTo(1);
        call(get("/api/prices").param("exhibitionId",Long.toString(f.exhibition())),null,null,200);
        call(post("/api/tickets"),owner,Map.of("termId",f.term(),"category","MISSING","paymentMethod","KARTICA"),409);
        call(post("/api/tickets"),owner,Map.of("termId",Long.MAX_VALUE,"category","REDOVNA","paymentMethod","KARTICA"),404);
        assertThatThrownBy(()->jdbc.update("update ulaznica set cena_placena=1 where id=?",id)).isInstanceOf(RuntimeException.class);
    }
    @Test void failedPaymentRetryRefundAndCapacityRelease() throws Exception {
        Fixture f=fixture(1);User owner=user();
        JsonNode ticket=call(post("/api/tickets"),owner,purchaseBody(f,true),201);long id=ticket.get("id").asLong();
        assertThat(ticket.get("status").asText()).isEqualTo("OTKAZANA");
        assertThat(call(get("/api/tickets/"+id+"/payments"),owner,null,200).get(0).get("status").asText()).isEqualTo("NEUSPESNA");
        jdbc.update("update cenovnik_izlozbe set cena=20 where izlozba_id=?",f.exhibition());
        JsonNode retried=call(post("/api/tickets/"+id+"/payments"),owner,Map.of("paymentMethod","KARTICA","demoDecline",false),200);
        assertThat(retried.get("paidPrice").decimalValue()).isEqualByComparingTo("20");
        call(post("/api/tickets/"+id+"/payments"),owner,Map.of("paymentMethod","KARTICA"),409);
        call(post("/api/tickets"),owner,purchaseBody(f,false),409);
        call(post("/api/tickets/"+id+"/cancel"),owner,null,200);
        JsonNode payments=call(get("/api/tickets/"+id+"/payments"),owner,null,200);
        assertThat(payments.get(1).get("status").asText()).isEqualTo("REFUNDIRANA");
        buy(f,owner);
    }
    @Test void concurrentPurchaseCannotOversellLastPlace() throws Exception {
        Fixture f=fixture(1);User a=user(),b=user();
        List<Integer> statuses=concurrent(List.of(a,b),u->post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(purchaseBody(f,false))));
        assertThat(statuses).containsExactlyInAnyOrder(201,409);
        assertThat(jdbc.queryForObject("select count(*) from ulaznica where termin_id=? and status<>'OTKAZANA'",Integer.class,f.term())).isEqualTo(1);
        assertThatThrownBy(()->jdbc.update("update termin_izlozbe set kapacitet=0 where id=?",f.term())).isInstanceOf(RuntimeException.class);
    }
    @Test void validationIsSingleUseEvenConcurrentlyAndRequiresCashier() throws Exception {
        Fixture f=fixture(2);User owner=user(),cashier=user("CASHIER");
        JsonNode ticket=buy(f,owner);
        assertThat(ticket.get("status").asText()).isEqualTo("KUPLJENA");
        assertThat(call(get("/api/tickets/"+ticket.get("id").asLong()+"/payments"),owner,null,200).get(0).get("status").asText()).isEqualTo("USPESNA");
        Map<String,Object> body=Map.of("qrToken",ticket.get("qrToken").asText(),"termId",f.term());
        call(post("/api/cashier/validate"),owner,body,403);
        call(post("/api/cashier/validate"),cashier,Map.of("qrToken",ticket.get("qrToken").asText(),"termId",Long.MAX_VALUE),409);
        assertThat(concurrent(List.of(cashier,cashier),u->post("/api/cashier/validate").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))))
                .containsExactlyInAnyOrder(201,409);
        call(post("/api/tickets/"+ticket.get("id").asLong()+"/cancel"),owner,null,409);
        assertThat(jdbc.queryForObject("select count(*) from poseta where ulaznica_id=?",Integer.class,ticket.get("id").asLong())).isEqualTo(1);
        assertThat(call(get("/api/tickets/"+ticket.get("id").asLong()),cashier,null,200).get("status").asText()).isEqualTo("AKTIVNA");
    }
    @Test void cashierCanSellAnonymousTicketWithoutInventingVisitor() throws Exception {
        Fixture f=fixture(2);User cashier=user("CASHIER"),visitor=user();
        JsonNode ticket=call(post("/api/cashier/tickets"),cashier,Map.of("termId",f.term(),"category","REDOVNA","paymentMethod","GOTOVINA"),201);
        assertThat(ticket.get("visitorId").isNull()).isTrue();
        long visit=enter(f,cashier,ticket);
        call(get("/api/visits/"+visit),visitor,null,403);
        call(get("/api/tickets/"+ticket.get("id").asLong()),visitor,null,403);
        call(post("/api/cashier/tickets"),visitor,purchaseBody(f,false),403);
        call(post("/api/tickets"),visitor,Map.of("termId",f.term(),"category","REDOVNA","paymentMethod","GOTOVINA"),400);
    }
    @Test void fullVisitScanRouteAndRatingLifecycle() throws Exception {
        Fixture f=fixture(2);User owner=user(),cashier=user("CASHIER"),other=user();
        long visit=enter(f,cashier,buy(f,owner));
        call(get("/api/visits/"+visit),other,null,403);
        call(post("/api/visits/"+visit+"/rating"),owner,Map.of("rating",5),409);
        JsonNode route=call(post("/api/visits/"+visit+"/routes"),owner,Map.of("criterion","KOMBINACIJA"),201);
        assertThat(route.get("items").size()).isEqualTo(2);
        assertThat(route.get("items").get(0).get("participationId").asLong()).isEqualTo(f.first());
        call(post("/api/visits/"+visit+"/scans"),other,Map.of("qrToken",f.firstQr()),403);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr(),"durationSeconds",-1),400);
        JsonNode scan=call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr(),"durationSeconds",30),201);
        assertThat(scan.get("next").get(0).get("participationId").asLong()).isEqualTo(f.second());
        assertThat(call(get("/api/visits/"+visit+"/routes"),owner,null,200).get(0).get("items").get(0).get("visited").asBoolean()).isTrue();
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.secondQr()),201);
        assertThat(call(get("/api/visits/"+visit+"/recommendations"),owner,null,200).isEmpty()).isTrue();
        call(post("/api/visits/"+visit+"/routes"),owner,Map.of("criterion","INTERESOVANJA"),409);
        call(post("/api/visits/"+visit+"/finish"),owner,null,200);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),409);
        call(post("/api/visits/"+visit+"/rating"),owner,Map.of("rating",6),400);
        call(post("/api/visits/"+visit+"/rating"),owner,Map.of("rating",5,"comment","Test"),201);
        call(post("/api/visits/"+visit+"/rating"),owner,Map.of("rating",4),409);
        call(get("/api/visits/"+visit+"/rating"),owner,null,200);
        assertThat(call(get("/api/visits/"+visit+"/scans"),owner,null,200).size()).isEqualTo(2);
        assertThat(call(get("/api/visits/me"),owner,null,200).size()).isEqualTo(1);
        verifyNoInteractions(graph);
    }
    @Test void unavailableAndForeignParticipationCannotBeScanned() throws Exception {
        Fixture f=fixture(2),foreign=fixture(2);User owner=user(),cashier=user("CASHIER");
        long visit=enter(f,cashier,buy(f,owner));
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",foreign.firstQr()),409);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken","missing"),404);
        jdbc.update("update ucesce_eksponata_na_izlozbi set datum_uklanjanja=now()-interval '1 minute' where id_ucesca=?",f.first());
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),409);
        JsonNode route=call(post("/api/visits/"+visit+"/routes"),owner,Map.of("criterion","BLIZINA"),201);
        assertThat(route.get("items").size()).isEqualTo(1);
        assertThatThrownBy(()->jdbc.update("insert into skeniranje_eksponata(poseta_id,ucesce_id,vreme_skeniranja) values (?,?,now())",visit,foreign.first())).isInstanceOf(RuntimeException.class);
    }
    @Test void scanReturnsContextAndNextAndRepeatedScanDoesNotRestoreVisitedCandidates() throws Exception {
        Fixture f=fixture(3);User owner=user(),cashier=user("CASHIER");
        long visit=enter(f,cashier,buy(f,owner));
        jdbc.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis='Context for this installation' where id_ucesca=?",f.first());
        jdbc.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,verzija,generisan_automatski,datum_kreiranja) values (?,'TEKST','/content/example.txt',1,false,now())",f.first());
        JsonNode full=call(post("/api/visits/"+visit+"/routes"),owner,Map.of("criterion","KOMBINACIJA"),201);
        assertThat(full.get("items").size()).isEqualTo(2);
        for(int i=0;i<2;i++) {
            JsonNode scan=call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),201);
            assertThat(scan.get("participation").get("name").asText()).isEqualTo("First");
            assertThat(scan.get("participation").get("contextDescription").asText()).isEqualTo("Context for this installation");
            assertThat(scan.get("participation").get("content").size()).isEqualTo(1);
            assertThat(scan.get("next").get(0).get("participationId").asLong()).isEqualTo(f.second());
            assertThat(scan.toString()).doesNotContain(f.firstQr());
        }
        assertThat(call(get("/api/visits/"+visit+"/scans"),owner,null,200).size()).isEqualTo(2);
        JsonNode remaining=call(post("/api/visits/"+visit+"/routes"),owner,Map.of("criterion","KOMBINACIJA"),201);
        assertThat(remaining.get("items").size()).isEqualTo(1);
        call(post("/api/visits/"+visit+"/scans"),null,Map.of("qrToken",f.firstQr()),401);
        call(post("/api/visits/"+Long.MAX_VALUE+"/scans"),owner,Map.of("qrToken",f.firstQr()),404);
        call(post("/api/visits/"+visit+"/finish"),owner,null,200);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),409);
    }
    @Test void demoEndpointsAreNotAvailableOutsideLocalProfile() throws Exception {
        User staff=user("CASHIER"),owner=user();
        call(get("/api/local-demo/board"),staff,null,403);
        call(get("/api/local-demo/enabled"),owner,null,403);
    }
    @Test void consentControlsGraphUseAndMissingGraphFallsBack() throws Exception {
        Fixture f=fixture(2);User owner=user(),cashier=user("CASHIER");
        long visit=enter(f,cashier,buy(f,owner));
        call(get("/api/visits/"+visit+"/recommendations"),owner,null,200);
        verifyNoInteractions(graph);
        jdbc.update("update posetilac set saglasnost_za_preporuke=true where nalog_id=?",owner.id());
        when(graph.scores(any(),anyList(),anyList(),any())).thenReturn(Map.of());
        JsonNode result=call(get("/api/visits/"+visit+"/recommendations"),owner,null,200);
        assertThat(result.get(0).get("participationId").asLong()).isEqualTo(f.first());
        verify(graph).scores(any(),anyList(),anyList(),any());
    }
    @Test void databasePreservesMinimumCardinalitiesAndHistory() throws Exception {
        Fixture f=fixture(2);User owner=user(),cashier=user("CASHIER");
        JsonNode ticket=buy(f,owner);long id=ticket.get("id").asLong(),visit=enter(f,cashier,ticket);
        assertThatThrownBy(()->jdbc.update("delete from ulaznica where id=?",id)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->jdbc.update("delete from transakcija where ulaznica_id=?",id)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->jdbc.update("insert into preporucena_ruta(poseta_id,datum_generisanja,kriterijum,status) values (?,now(),'KOMBINACIJA','AKTIVNA')",visit)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->jdbc.update("""
            insert into ulaznica(posetilac_id,termin_id,cenovnik_izlozba_id,cenovnik_vazi_od,kategorija_cene,datum_kupovine,cena_placena,status,qr_token,vazi_do)
            select posetilac_id,termin_id,cenovnik_izlozba_id,cenovnik_vazi_od,kategorija_cene,datum_kupovine,cena_placena,'OTKAZANA',?,vazi_do from ulaznica where id=?
            """,UUID.randomUUID().toString(),id)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->jdbc.update("insert into ocena_izlozbe(poseta_id,ocena,datum_ocene) values (?,5,now())",visit)).isInstanceOf(RuntimeException.class);
    }
    @Test void expiryClosesVisitAndBlocksLateEntry() throws Exception {
        Fixture f=fixture(3);User owner=user(),cashier=user("CASHIER");
        JsonNode active=buy(f,owner),unused=buy(f,owner);long visit=enter(f,cashier,active);
        jdbc.update("update termin_izlozbe set kraj=now()-interval '1 minute' where id=?",f.term());
        call(post("/api/cashier/validate"),cashier,Map.of("qrToken",unused.get("qrToken").asText(),"termId",f.term()),409);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),409);
        maintenance.advanceStatuses();
        assertThat(call(get("/api/visits/"+visit),owner,null,200).get("status").asText()).isEqualTo("ZAVRSENA");
    }
    @Test void futureTermCannotBeEnteredAndClosedTermCannotBeSold() throws Exception {
        Fixture f=fixture(2);User owner=user(),cashier=user("CASHIER");
        jdbc.update("update termin_izlozbe set pocetak=now()+interval '10 minutes' where id=?",f.term());
        JsonNode ticket=buy(f,owner);
        call(post("/api/cashier/validate"),cashier,Map.of("qrToken",ticket.get("qrToken").asText(),"termId",f.term()),409);
        jdbc.update("update termin_izlozbe set status='OTKAZAN' where id=?",f.term());
        call(post("/api/tickets"),owner,purchaseBody(f,false),409);
        call(post("/api/tickets"),null,purchaseBody(f,false),401);
        call(get("/api/visits/"+Long.MAX_VALUE),owner,null,404);
    }
    @Test void futureExhibitionsAndContentRespectVisibilityOwnershipAndConsent() throws Exception {
        Fixture f=fixture(2);User owner=user(),cashier=user("CASHIER");
        long visit=enter(f,cashier,buy(f,owner));
        jdbc.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,verzija,generisan_automatski,datum_kreiranja) values (?,'TEKST','/content/test',1,false,now())",f.first());
        JsonNode content=call(get("/api/visits/"+visit+"/content/"+f.first()),owner,null,200);
        assertThat(content.get("content").get(0).get("type").asText()).isEqualTo("TEKST");
        call(get("/api/recommendations/exhibitions"),owner,null,200);
        verifyNoInteractions(graph);
        jdbc.update("update posetilac set saglasnost_za_preporuke=true where nalog_id=?",owner.id());
        when(graph.scores(any(),anyList(),anyList())).thenReturn(Map.of());
        call(get("/api/recommendations/exhibitions"),owner,null,200);
        verify(graph).scores(any(),anyList(),anyList());
        call(post("/api/visits/"+visit+"/finish"),owner,null,200);
        call(get("/api/visits/"+visit+"/content/"+f.first()),owner,null,409);
    }
    @Test void graphProjectionQueriesReadActualPostgresSchema() {
        org.neo4j.driver.Driver driver=mock(org.neo4j.driver.Driver.class);
        org.neo4j.driver.Session session=mock(org.neo4j.driver.Session.class);
        when(driver.session()).thenReturn(session);
        new GraphProjection(driver,jdbc).synchronize();
        verify(driver).session();
    }
    @FunctionalInterface private interface Request { MockHttpServletRequestBuilder create(User user) throws Exception; }
    private List<Integer> concurrent(List<User> users,Request request) throws Exception {
        CountDownLatch start=new CountDownLatch(1);
        try(ExecutorService executor=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> futures=new ArrayList<>();
            for(User user:users)futures.add(executor.submit(()->{
                start.await();
                return mvc.perform(request.create(user).header("Authorization","Bearer "+user.token())).andReturn().getResponse().getStatus();
            }));
            start.countDown();
            return List.of(futures.get(0).get(40,TimeUnit.SECONDS),futures.get(1).get(40,TimeUnit.SECONDS));
        }
    }
}
