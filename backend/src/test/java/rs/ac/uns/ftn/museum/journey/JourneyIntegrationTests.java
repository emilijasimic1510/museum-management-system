package rs.ac.uns.ftn.museum.journey;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.Instant;
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

@SpringBootTest(properties={"museum.tts.espeak-path=./target/nonexistent-espeak-regression", "museum.audio.storage-path=./target/tts-failure-regression"})
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
    private JsonNode browseExhibition(long id) throws Exception {
        for (JsonNode e : call(get("/api/exhibitions").param("browse","true"),null,null,200))
            if(e.path("id").asLong()==id)return e;
        return json.nullNode();
    }
    @Test void pastExhibitionIsAbsentFromBrowseButPreservedForHistory() throws Exception {
        Fixture f=fixture(3);
        assertThat(browseExhibition(f.exhibition()).isNull()).isFalse();
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            jdbc.update("update termin_izlozbe set pocetak=now()-interval '2 days',kraj=now()-interval '1 day' where id=?",f.term());
            jdbc.update("update izlozba set datum_pocetka=now()-interval '3 days',datum_zavrsetka=now()-interval '1 day' where id=?",f.exhibition());
        });
        // Date filtering must work even before a maintenance job updates status.
        assertThat(browseExhibition(f.exhibition()).isNull()).isTrue();
        assertThat(call(get("/api/exhibitions/"+f.exhibition()),null,null,200).path("terms")).hasSize(1);
        long exhibit=jdbc.queryForObject("select eksponat_id from ucesce_eksponata_na_izlozbi where id_ucesca=?",Long.class,f.first());
        assertThat(call(get("/api/exhibits/"+exhibit),null,null,200).path("exhibitions")).isEmpty();
        assertThat(call(get("/api/exhibits/"+exhibit+"/participations"),null,null,200)).isEmpty();
    }
    @Test void futureSoldOutTermRemainsBrowsableButCannotBePurchased() throws Exception {
        Fixture f=fixture(1);User owner=user(),other=user();
        jdbc.update("update termin_izlozbe set pocetak=now()+interval '1 hour',kraj=now()+interval '2 hours' where id=?",f.term());
        buy(f,owner);
        assertThat(browseExhibition(f.exhibition()).path("terms").get(0).path("status").asText()).isEqualTo("POPUNJEN");
        call(post("/api/tickets"),other,purchaseBody(f,false),409);
    }
    @Test void browseOmitsExpiredTermsAndFinishedExhibitionStatuses() throws Exception {
        Fixture f=fixture(3);
        jdbc.update("update termin_izlozbe set pocetak=now()-interval '2 hours',kraj=now()-interval '1 hour' where id=?",f.term());
        assertThat(browseExhibition(f.exhibition()).path("terms")).isEmpty();
        for(String state:List.of("ZAVRSENA","OTKAZANA")) {
            jdbc.update("update izlozba set status=? where id=?",state,f.exhibition());
            assertThat(browseExhibition(f.exhibition()).isNull()).isTrue();
        }
    }
    @Test void pastTicketAndFinishedVisitRemainInVisitorHistory() throws Exception {
        Fixture f=fixture(3);User owner=user(),cashier=user("CASHIER");
        JsonNode ticket=buy(f,owner);long id=ticket.path("id").asLong();
        long visit=enter(f,cashier,ticket);
        call(post("/api/visits/"+visit+"/finish"),owner,null,200);
        jdbc.update("update ulaznica set datum_kupovine=now()-interval '2 days',vazi_do=now()-interval '1 day' where id=?",id);
        jdbc.update("update izlozba set status='ZAVRSENA' where id=?",f.exhibition());
        assertThat(browseExhibition(f.exhibition()).isNull()).isTrue();
        assertThat(call(get("/api/tickets/me"),owner,null,200).get(0).path("id").asLong()).isEqualTo(id);
        JsonNode history=call(get("/api/visits/me"),owner,null,200).get(0);
        assertThat(history.path("id").asLong()).isEqualTo(visit);
        assertThat(history.path("status").asText()).isEqualTo("ZAVRSENA");
    }
    @Test void pastEventLeavesPublicBrowseButRegistrationHistoryRemains() throws Exception {
        Fixture f=fixture(3);User guide=user("GUIDE"),owner=user(),other=user();
        long event=call(post("/api/events"),guide,Map.of("exhibitionId",f.exhibition(),"name","Browse regression","type","TURA",
                "start",Instant.now().plusSeconds(3600).toString(),"end",Instant.now().plusSeconds(7200).toString(),"capacity",1,"status","PLANIRAN"),201).path("id").asLong();
        call(post("/api/events/"+event+"/registrations"),owner,Map.of(),201);
        JsonNode future=call(get("/api/events").param("browse","true"),null,null,200);
        assertThat(future).anySatisfy(e->{assertThat(e.path("id").asLong()).isEqualTo(event);assertThat(e.path("status").asText()).isEqualTo("POPUNJEN");});
        call(post("/api/events/"+event+"/registrations"),other,Map.of(),409);
        jdbc.update("update dogadjaj set pocetak=now()-interval '2 hours',kraj=now()-interval '1 hour' where id=?",event);
        assertThat(call(get("/api/events").param("browse","true"),null,null,200)).noneSatisfy(e->assertThat(e.path("id").asLong()).isEqualTo(event));
        assertThat(call(get("/api/events"),guide,null,200)).anySatisfy(e->assertThat(e.path("id").asLong()).isEqualTo(event));
        JsonNode registration=call(get("/api/visitors/me/registrations"),owner,null,200).get(0);
        assertThat(registration.path("eventId").asLong()).isEqualTo(event);
        assertThat(registration.path("status").asText()).isEqualTo("PRIJAVLJEN");
    }
    private long enter(Fixture f,User cashier,JsonNode ticket) throws Exception {
        return call(post("/api/cashier/validate"),cashier,Map.of("qrToken",ticket.get("qrToken").asText(),"termId",f.term()),201).get("id").asLong();
    }
    @Test void missingLocalTtsDoesNotBlockCatalogLoginSaleValidationOrVisit() throws Exception {
        Fixture f=fixture(3);User curator=user("CURATOR"),owner=user(),cashier=user("CASHIER");
        jdbc.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis='Tekst konkretne postavke' where id_ucesca=?",f.first());
        var failure=call(post("/api/curator/participations/"+f.first()+"/audio"),curator,Map.of("language","sr"),503);
        assertThat(failure.get("code").asText()).isEqualTo("LOCAL_AUDIO_ERROR");
        assertThat(failure.get("detail").asText()).contains("eSpeak NG");
        assertThat(jdbc.queryForObject("select count(*) from digitalni_sadrzaj where ucesce_id=?",Long.class,f.first())).isZero();
        call(get("/api/exhibitions/"+f.exhibition()),null,null,200);
        long visit=enter(f,cashier,buy(f,owner));
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),201);
        assertThat(call(get("/api/visits/"+visit+"/content/"+f.first()),owner,null,200).get("contextDescription").asText()).isEqualTo("Tekst konkretne postavke");
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
    @Test void finishedHistoryAndRatingAreOwnedPersistentAndSingleUse() throws Exception {
        Fixture f=fixture(2);User owner=user(),other=user(),cashier=user("CASHIER");
        long visit=enter(f,cashier,buy(f,owner));String path="/api/visits/"+visit;
        call(post(path+"/rating"),owner,Map.of("rating",5),409);
        call(post(path+"/scans"),owner,Map.of("qrToken",f.firstQr()),201);
        JsonNode closed=call(post(path+"/finish"),owner,null,200);
        assertThat(closed.path("status").asText()).isEqualTo("ZAVRSENA");
        assertThat(closed.path("exitedAt").isTextual()).isTrue();
        assertThat(closed.path("exhibitionId").asLong()).isEqualTo(f.exhibition());
        assertThat(closed.path("exhibitionName").asText()).isEqualTo("Journey");
        // PostgreSQL timestamps have microsecond precision; compare persisted responses for idempotence.
        closed=call(get(path),owner,null,200);
        assertThat(call(post(path+"/finish"),owner,null,200)).isEqualTo(closed);
        maintenance.advanceStatuses();
        assertThat(call(get(path),owner,null,200)).isEqualTo(closed);
        assertThat(call(get("/api/visits/me"),owner,null,200)).contains(closed);
        assertThat(call(get("/api/visits/me"),other,null,200)).isEmpty();
        call(post(path+"/scans"),owner,Map.of("qrToken",f.secondQr()),409);
        JsonNode history=call(get(path+"/scans"),owner,null,200);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).path("participation").path("name").asText()).isEqualTo("First");
        assertThat(history.get(0).path("participation").path("content")).isEmpty();
        for(int invalid:new int[]{0,6})call(post(path+"/rating"),owner,Map.of("rating",invalid),400);
        call(post(path+"/rating"),other,Map.of("rating",3),403);
        call(post(path+"/finish"),other,null,403);
        call(get(path+"/scans"),other,null,403);
        JsonNode rating=call(post(path+"/rating"),owner,Map.of("rating",1,"comment","Posebno mi se dopao raspored postavke."),201);
        assertThat(rating.path("visitId").asLong()).isEqualTo(visit);
        assertThat(rating.path("rating").asInt()).isEqualTo(1);
        assertThat(rating.path("comment").asText()).isEqualTo("Posebno mi se dopao raspored postavke.");
        assertThat(rating.path("ratedAt").isTextual()).isTrue();
        JsonNode storedRating=call(get(path+"/rating"),owner,null,200);
        for(String field:List.of("id","visitId","rating","comment"))assertThat(storedRating.path(field)).isEqualTo(rating.path(field));
        assertThat(java.time.Duration.between(java.time.Instant.parse(rating.path("ratedAt").asText()),
                java.time.Instant.parse(storedRating.path("ratedAt").asText())).abs()).isLessThan(java.time.Duration.ofMillis(1));
        call(post(path+"/rating"),owner,Map.of("rating",5),409);
        call(get(path+"/rating"),other,null,403);
        call(get("/api/visits/me"),null,null,401);
        call(get(path+"/rating"),null,null,401);
        call(post(path+"/rating"),null,Map.of("rating",5),401);
        for(String role:List.of("CASHIER","GUIDE","CURATOR")) {
            User staff=user(role);
            jdbc.update("delete from nalog_uloga where nalog_id=? and uloga_id=(select id from uloga where naziv='VISITOR')",staff.id());
            call(get("/api/visits/me"),staff,null,403);
            call(get(path+"/rating"),staff,null,403);
            call(post(path+"/rating"),staff,Map.of("rating",5),403);
        }
    }
    @Test void futureRecommendationsUseCompletedScansOnlyWithConsentAndKeepGeneralFallback() throws Exception {
        Fixture f=fixture(2);User owner=user(),cashier=user("CASHIER");
        long visit=enter(f,cashier,buy(f,owner));
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),201);
        call(post("/api/visits/"+visit+"/finish"),owner,null,200);
        JsonNode general=call(get("/api/recommendations/exhibitions"),owner,null,200);
        assertThat(general).isNotEmpty();
        assertThat(general.findValuesAsText("explanation")).allMatch(s->s.contains("opšti pregled"));
        verifyNoInteractions(graph);
        jdbc.update("update posetilac set saglasnost_za_preporuke=true where nalog_id=?",owner.id());
        long previous=jdbc.queryForObject("select eksponat_id from ucesce_eksponata_na_izlozbi where id_ucesca=?",Long.class,f.first());
        when(graph.scores(any(),anyList(),anyList())).thenReturn(Map.of());
        JsonNode fallback=call(get("/api/recommendations/exhibitions"),owner,null,200);
        assertThat(fallback.findValuesAsText("explanation")).allMatch(s->s.contains("opšti pregled"));
        verify(graph).scores(eq(previous),anyList(),anyList());
        clearInvocations(graph);
        jdbc.update("update posetilac set saglasnost_za_preporuke=false where nalog_id=?",owner.id());
        assertThat(call(get("/api/recommendations/exhibitions"),owner,null,200)).isEqualTo(general);
        verifyNoInteractions(graph);
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
        call(get("/api/visits/"+visit+"/content/"+f.first()),owner,null,403);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),201);
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
    @Test void scanUnlockIsScopedToParticipationVisitorAndActiveVisit() throws Exception {
        Fixture a=fixture(4),b=fixture(4);User owner=user(),other=user(),cashier=user("CASHIER");
        long exhibit=jdbc.queryForObject("select eksponat_id from ucesce_eksponata_na_izlozbi where id_ucesca=?",Long.class,a.first());
        jdbc.update("update ucesce_eksponata_na_izlozbi set eksponat_id=? where id_ucesca=?",exhibit,b.first());
        jdbc.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis='Context A' where id_ucesca=?",a.first());
        for(long participation:List.of(a.first(),b.first()))jdbc.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,verzija,generisan_automatski,datum_kreiranja) values (?,'TEKST',?,1,false,now())",participation,"/content/"+participation);
        String summary="/api/visits/me/exhibits/"+exhibit+"/content";
        call(get(summary),null,null,401);
        assertThat(call(get(summary),owner,null,200).get(0).get("state").asText()).isEqualTo("NO_ACTIVE_VISIT");
        long visitA=enter(a,cashier,buy(a,owner)),visitB=enter(b,cashier,buy(b,owner));
        String contentA="/api/visits/"+visitA+"/content/"+a.first();
        call(get(contentA),owner,null,403);
        call(get(contentA),other,null,403);
        JsonNode locked=call(get(summary),owner,null,200);
        assertThat(locked.get(0).get("state").asText()).isEqualTo("SCAN_REQUIRED");
        assertThat(locked.toString()).doesNotContain("/content/","Context A");
        call(post("/api/visits/"+visitA+"/scans"),owner,Map.of("qrToken",a.firstQr()),201);
        assertThat(jdbc.queryForObject("select count(*) from skeniranje_eksponata where poseta_id=? and ucesce_id=?",Integer.class,visitA,a.first())).isEqualTo(1);
        JsonNode unlocked=call(get(contentA),owner,null,200);
        assertThat(unlocked.get("contextDescription").asText()).isEqualTo("Context A");
        assertThat(unlocked.get("content").get(0).get("path").asText()).isEqualTo("/content/"+a.first());
        call(get("/api/visits/"+visitA+"/content/"+a.second()),owner,null,403);
        call(get("/api/visits/"+visitA+"/content/"+b.first()),owner,null,409);
        call(get("/api/visits/"+visitB+"/content/"+b.first()),owner,null,403);
        JsonNode split=call(get(summary),owner,null,200);
        assertThat(split.get(0).get("state").asText()).isEqualTo("UNLOCKED");
        assertThat(split.get(1).get("state").asText()).isEqualTo("SCAN_REQUIRED");
        assertThat(split.toString()).doesNotContain("/content/"+b.first());
        assertThat(call(get(summary),other,null,200).toString()).doesNotContain("Context A","/content/");
        call(post("/api/visits/"+visitB+"/scans"),owner,Map.of("qrToken",b.firstQr()),201);
        assertThat(call(get("/api/visits/"+visitB+"/content/"+b.first()),owner,null,200).get("content").get(0).get("path").asText()).isEqualTo("/content/"+b.first());
        call(post("/api/visits/"+visitA+"/finish"),owner,null,200);
        call(get(contentA),owner,null,409);
        assertThat(call(get("/api/visits/"+visitA+"/scans"),owner,null,200).toString()).doesNotContain("Context A","/content/");
        assertThat(call(get(summary),owner,null,200).get(0).get("state").asText()).isEqualTo("NO_ACTIVE_VISIT");
        long newVisit=enter(a,cashier,buy(a,owner));
        call(get("/api/visits/"+newVisit+"/content/"+a.first()),owner,null,403);
        assertThat(call(get(summary),owner,null,200).get(0).get("state").asText()).isEqualTo("SCAN_REQUIRED");
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
