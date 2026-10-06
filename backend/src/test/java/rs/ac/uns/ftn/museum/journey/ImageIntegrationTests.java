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
class ImageIntegrationTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JourneyMaintenance maintenance;
    @MockitoBean GraphProjection graph;
    @MockitoBean rs.ac.uns.ftn.museum.audio.TtsService tts;
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path audioRoot;
    @org.springframework.test.context.DynamicPropertySource static void audioStorage(org.springframework.test.context.DynamicPropertyRegistry r){r.add("museum.image.storage-path",()->audioRoot.toString());}
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
    private JsonNode upload(String url,User user) throws Exception {
        var file=new org.springframework.mock.web.MockMultipartFile("file","image.png","image/png",rs.ac.uns.ftn.museum.config.LocalImageSeeder.illustration(3));
        return call(multipart(url).file(file),user,null,201);
    }
    @Test void imageUploadPublicAndPrivateAuthorization()throws Exception {
        Fixture f=fixture(4);User curator=user("CURATOR"),owner=user(),other=user(),cashier=user("CASHIER");
        String upload="/api/curator/participations/"+f.first()+"/images";
        var file=new org.springframework.mock.web.MockMultipartFile("file","x.png","image/png",rs.ac.uns.ftn.museum.config.LocalImageSeeder.illustration(1));
        call(multipart(upload).file(file),null,null,401);
        for(User denied:List.of(owner,cashier,user("GUIDE"),user("MANAGER")))call(multipart(upload).file(file),denied,null,403);
        call(multipart(upload).file(new org.springframework.mock.web.MockMultipartFile("file","x.svg","image/svg+xml","<svg/>".getBytes())),curator,null,415);
        call(multipart(upload).file(new org.springframework.mock.web.MockMultipartFile("file","x.png","image/png",new byte[5242881])),curator,null,413);
        long content=upload(upload,curator).get("contentId").asLong();
        assertThat(call(get("/api/digital-content/"+content),curator,null,200).get("path").asText()).isEmpty();
        call(put("/api/digital-content/"+content),curator,Map.of("participationId",f.first(),"type","SLIKA","path","demo/other.png","version",1,"automaticallyGenerated",false),400);
        String curatorUrl=upload+"/"+content;
        mvc.perform(get(curatorUrl).header("Authorization","Bearer "+curator.token())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        call(get(curatorUrl),owner,null,403);
        long visit=enter(f,cashier,buy(f,owner));
        String download="/api/visits/"+visit+"/content/"+f.first()+"/images/"+content;
        call(get(download),owner,null,403);call(get(download),other,null,403);call(get(download),null,null,401);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),201);
        mvc.perform(get(download).header("Authorization","Bearer "+owner.token())).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store"));
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.secondQr()),201);
        call(get("/api/visits/"+visit+"/content/"+f.second()+"/images/"+content),owner,null,404);
        call(post("/api/visits/"+visit+"/finish"),owner,Map.of(),200);call(get(download),owner,null,409);
        upload("/api/curator/media/exhibitions/"+f.exhibition(),curator);
        mvc.perform(get("/api/media/exhibitions/"+f.exhibition())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        jdbc.update("update izlozba set online_vidljiva=false where id=?",f.exhibition());
        call(get("/api/media/exhibitions/"+f.exhibition()),null,null,404);
        long exhibit=jdbc.queryForObject("select eksponat_id from ucesce_eksponata_na_izlozbi where id_ucesca=?",Long.class,f.first());
        upload("/api/curator/media/exhibits/"+exhibit,curator);
        mvc.perform(get("/api/media/exhibits/"+exhibit)).andExpect(status().isOk());
        call(get("/api/media/exhibits/9223372036854775807"),null,null,404);
    }
}
