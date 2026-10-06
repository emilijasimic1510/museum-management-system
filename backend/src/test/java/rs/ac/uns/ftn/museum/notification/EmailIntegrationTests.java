package rs.ac.uns.ftn.museum.notification;

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

@SpringBootTest(properties={"museum.mail.enabled=true","museum.mail.from=museum@example.test"})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@ActiveProfiles("test")
class EmailIntegrationTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JourneyMaintenance maintenance;
    @MockitoBean GraphProjection graph;
    @MockitoBean org.springframework.mail.javamail.JavaMailSender sender;
    private final List<jakarta.mail.internet.MimeMessage> sent=new ArrayList<>();
    @BeforeEach void transport() {
        sent.clear();
        when(sender.createMimeMessage()).thenAnswer(i->new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties())));
        doAnswer(i->{sent.add(i.getArgument(0));return null;}).when(sender).send(any(jakarta.mail.internet.MimeMessage.class));
    }
    private String text(jakarta.mail.Part part) throws Exception {
        Object content=part.getContent();
        if(content instanceof jakarta.mail.Multipart multipart) {
            String result="";for(int i=0;i< multipart.getCount();i++)result+=text(multipart.getBodyPart(i));return result;
        }
        return content.toString();
    }
    private long event(Fixture f) {
        return jdbc.queryForObject("insert into dogadjaj(izlozba_id,naziv,tip,pocetak,kraj,kapacitet,status) values (?,'Museum tour','TURA',now()+interval '1 hour',now()+interval '2 hours',5,'PLANIRAN') returning id",Long.class,f.exhibition());
    }
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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void allFiveOperationsCommitEvenWhenSmtpFails(boolean fail) throws Exception {
        if(fail)doAnswer(i->{sent.add(i.getArgument(0));throw new org.springframework.mail.MailSendException("SMTP unavailable");})
                .when(sender).send(any(jakarta.mail.internet.MimeMessage.class));
        Fixture f=fixture(5);User owner=user();
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getSubject()).contains("Dobro došli");
        String recipient=jdbc.queryForObject("select email from korisnicki_nalog where id=?",String.class,owner.id());
        assertThat(sent.getFirst().getAllRecipients()[0].toString()).isEqualTo(recipient);
        User cashier=user("CASHIER");sent.clear();
        var ticket=call(post("/api/tickets").header("X-Museum-Language","en"),owner,purchaseBody(f,false),201);
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getSubject()).isEqualTo("Ticket purchase confirmed");
        assertThat(text(sent.getFirst())).contains("Journey","12.50","REDOVNA").doesNotContain(ticket.path("qrToken").asText());
        assertThat(call(get("/api/tickets/"+ticket.path("id").asLong()),owner,null,200).path("status").asText()).isEqualTo("KUPLJENA");
        long event=event(f);
        call(post("/api/events/"+event+"/registrations").header("X-Museum-Language","en"),owner,null,201);
        assertThat(sent).hasSize(2);assertThat(sent.getLast().getSubject()).isEqualTo("Event registration confirmed");
        call(post("/api/events/"+event+"/registrations"),owner,null,409);
        assertThat(sent).hasSize(2);
        assertThat(call(get("/api/visitors/me/registrations"),owner,null,200).get(0).path("status").asText()).isEqualTo("PRIJAVLJEN");
        call(delete("/api/events/"+event+"/registrations/me").header("X-Museum-Language","en"),owner,null,204);
        assertThat(sent).hasSize(3);assertThat(sent.getLast().getSubject()).isEqualTo("Event cancellation confirmed");
        call(delete("/api/events/"+event+"/registrations/me"),owner,null,204);
        assertThat(sent).hasSize(3);
        assertThat(call(get("/api/visitors/me/registrations"),owner,null,200).get(0).path("status").asText()).isEqualTo("OTKAZAN");
        long visit=enter(f,cashier,ticket);
        call(post("/api/visits/"+visit+"/finish").header("X-Museum-Language","en"),owner,null,200);
        assertThat(sent).hasSize(4);assertThat(sent.getLast().getSubject()).isEqualTo("Thank you for visiting");
        call(post("/api/visits/"+visit+"/finish"),owner,null,200);
        assertThat(sent).hasSize(4);
        assertThat(call(get("/api/visits/"+visit),owner,null,200).path("status").asText()).isEqualTo("ZAVRSENA");
        assertThat(sent).allSatisfy(m->{try{assertThat(m.getAllRecipients()[0].toString()).isEqualTo(recipient);}catch(Exception ex){throw new AssertionError(ex);}});
    }
    @Test void failedPaymentAndAnonymousSaleDoNotSendButSuccessfulRetryDoes() throws Exception {
        Fixture f=fixture(5);User owner=user(),cashier=user("CASHIER");sent.clear();
        var declined=call(post("/api/tickets"),owner,purchaseBody(f,true),201);
        assertThat(sent).isEmpty();
        call(post("/api/tickets/"+declined.path("id").asLong()+"/payments"),owner,Map.of("paymentMethod","KARTICA","demoDecline",false),200);
        assertThat(sent).hasSize(1);sent.clear();
        call(post("/api/cashier/tickets"),cashier,Map.of("termId",f.term(),"category","REDOVNA","paymentMethod","GOTOVINA"),201);
        assertThat(sent).isEmpty();
    }
    @Test void automaticCompletionSendsOnceWithSerbianFallback() throws Exception {
        Fixture f=fixture(3);User owner=user(),cashier=user("CASHIER");
        long visit=enter(f,cashier,buy(f,owner));sent.clear();
        jdbc.update("update termin_izlozbe set kraj=now()-interval '1 minute' where id=?",f.term());
        maintenance.advanceStatuses();maintenance.advanceStatuses();
        assertThat(sent).hasSize(1);assertThat(sent.getFirst().getSubject()).isEqualTo("Hvala na poseti");
        assertThat(call(get("/api/visits/"+visit),owner,null,200).path("status").asText()).isEqualTo("ZAVRSENA");
    }
    @Test void realTransactionRollbackDoesNotSendRegistration() {
        String email=UUID.randomUUID()+"@example.test";
        var template=new TransactionTemplate(transactions);
        template.executeWithoutResult(status->{
            accounts.register(new rs.ac.uns.ftn.museum.people.dto.RegisterRequest(email,UUID.randomUUID().toString(),"Test","Visitor",null,false));
            assertThat(sent).isEmpty();status.setRollbackOnly();
        });
        assertThat(sent).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from korisnicki_nalog where email=?",Integer.class,email)).isZero();
    }
    @Autowired rs.ac.uns.ftn.museum.people.service.AccountService accounts;
}
