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
class AudioIntegrationTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JourneyMaintenance maintenance;
    @MockitoBean GraphProjection graph;
    @MockitoBean rs.ac.uns.ftn.museum.audio.TtsService tts;
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path audioRoot;
    @org.springframework.test.context.DynamicPropertySource static void audioStorage(org.springframework.test.context.DynamicPropertyRegistry r){r.add("museum.audio.storage-path",()->audioRoot.toString());}
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
    @Test void generationAndScanProtectedDownload()throws Exception {
        Fixture f=fixture(4);User curator=user("CURATOR"),owner=user(),other=user(),cashier=user("CASHIER");
        jdbc.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis='Kontekst za audio' where id_ucesca=?",f.first());
        doAnswer(a->{try(var in=new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(new byte[160]),new javax.sound.sampled.AudioFormat(8000,16,1,true,false),80)){javax.sound.sampled.AudioSystem.write(in,javax.sound.sampled.AudioFileFormat.Type.WAVE,((java.nio.file.Path)a.getArgument(2)).toFile());}return null;}).when(tts).generate(anyString(),eq("sr"),any());
        String generate="/api/curator/participations/"+f.first()+"/audio";
        for(User denied:List.of(owner,cashier,user("GUIDE"),user("MANAGER")))call(post(generate),denied,Map.of("language","sr"),403);
        call(post(generate),null,Map.of("language","sr"),401);
        var first=call(post(generate),curator,Map.of("language","sr"),201);
        assertThat(first.get("participationId").asLong()).isEqualTo(f.first());assertThat(first.get("path").asText()).isEmpty();assertThat(first.get("automaticallyGenerated").asBoolean()).isTrue();
        assertThat(call(post(generate),curator,Map.of("language","sr"),201).get("version").asInt()).isEqualTo(2);
        call(post("/api/curator/participations/"+f.second()+"/audio"),curator,Map.of("language","sr"),400);
        long count=jdbc.queryForObject("select count(*) from digitalni_sadrzaj where ucesce_id=?",Long.class,f.first());
        doThrow(new rs.ac.uns.ftn.museum.audio.AudioException("TTS failure")).when(tts).generate(anyString(),eq("sr"),any());
        call(post(generate),curator,Map.of("language","sr"),503);
        assertThat(jdbc.queryForObject("select count(*) from digitalni_sadrzaj where ucesce_id=?",Long.class,f.first())).isEqualTo(count);
        long visit=enter(f,cashier,buy(f,owner));String download="/api/visits/"+visit+"/content/"+f.first()+"/audio/"+first.get("id").asLong();
        call(get(download),owner,null,403);call(get(download),other,null,403);
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.firstQr()),201);
        mvc.perform(get(download).header("Authorization","Bearer "+owner.token())).andExpect(status().isOk()).andExpect(content().contentType("audio/wav")).andExpect(header().string("Cache-Control","no-store"));
        call(post("/api/visits/"+visit+"/scans"),owner,Map.of("qrToken",f.secondQr()),201);
        call(get("/api/visits/"+visit+"/content/"+f.second()+"/audio/"+first.get("id").asLong()),owner,null,404);
        call(post("/api/visits/"+visit+"/finish"),owner,Map.of(),200);call(get(download),owner,null,409);
    }
}
