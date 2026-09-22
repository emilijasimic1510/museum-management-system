package rs.ac.uns.ftn.museum.journey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import rs.ac.uns.ftn.museum.config.DemoDataSeeder;
import rs.ac.uns.ftn.museum.journey.service.GraphProjection;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@ActiveProfiles({"test","local"})
class LocalDemoJourneyTests extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoDataSeeder seed;
    @MockitoBean GraphProjection graph;
    private JsonNode call(MockHttpServletRequestBuilder request,String token,Object body,int expected) throws Exception {
        if(token!=null)request.header("Authorization","Bearer "+token);
        if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String result=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return result.isBlank()?json.nullNode():json.readTree(result);
    }
    private String login(String email) throws Exception {
        return call(post("/api/auth/login"),null,Map.of("email",email,"password",DemoDataSeeder.PASSWORD),200).get("accessToken").asText();
    }
    @Test void catalogSeedIsConnectedAndIdempotent() {
        seed.seed();
        var before=jdbc.queryForList("select autor_id,eksponat_id from autorstvo order by autor_id,eksponat_id");
        Integer contents=jdbc.queryForObject("select count(*) from digitalni_sadrzaj",Integer.class);
        seed.seed();
        assertThat(jdbc.queryForList("select autor_id,eksponat_id from autorstvo order by autor_id,eksponat_id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from digitalni_sadrzaj",Integer.class)).isEqualTo(contents);
        assertThat(jdbc.queryForObject("select count(*) from autor where (ime='Mila' and prezime='Jovanović') or (ime='Luka' and prezime='Petrović')",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from eksponat where inventarski_broj like 'DEMO-%'",Integer.class)).isGreaterThanOrEqualTo(9);
        assertThat(jdbc.queryForObject("select count(distinct p.izlozba_id) from ucesce_eksponata_na_izlozbi p join eksponat e on e.id=p.eksponat_id where e.inventarski_broj like 'DEMO-%'",Integer.class)).isGreaterThanOrEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from digitalni_sadrzaj where putanja='demo/pre-nego-sto-svane.mp3'",Integer.class)).isZero();
    }
    @Test void everyLiveDemoExhibitionHasCompatibleScansRecommendationsAndRoute() throws Exception {
        seed.seed();seed.seed();
        String visitor=login(DemoDataSeeder.VISITOR_EMAIL),staff=login(DemoDataSeeder.STAFF_EMAIL);
        JsonNode board=call(get("/api/local-demo/board"),staff,null,200);
        assertThat(board.size()).isEqualTo(9);
        call(get("/api/local-demo/board"),visitor,null,403);
        assertThat(call(get("/api/exhibitions"),null,null,200).toString()).doesNotContain("qrToken","DEMO-QR","DEMO-SCULPTURE-QR");
        for(String name:new String[]{"Tragovi svetlosti","Veče skulpture","Grad kroz objektiv"}) {
            Long exhibition=jdbc.queryForObject("select id from izlozba where naziv=?",Long.class,name);
            Long term=jdbc.queryForObject("select id from termin_izlozbe where izlozba_id=? and pocetak<=now() and kraj>now() and status='OTVOREN' order by id limit 1",Long.class,exhibition);
            JsonNode ticket=call(post("/api/tickets"),visitor,Map.of("termId",term,"category","ODRASLI","paymentMethod","KARTICA"),201);
            long visit=call(post("/api/cashier/validate"),staff,Map.of("qrToken",ticket.get("qrToken").asText(),"termId",term),201).get("id").asLong();
            JsonNode entries=call(get("/api/local-demo/visits/"+visit),visitor,null,200);
            assertThat(entries.size()).isEqualTo(3);
            for(JsonNode entry:entries)assertThat(entry.get("exhibitionId").asLong()).isEqualTo(exhibition);
            String foreign=name.equals("Tragovi svetlosti")?"DEMO-SCULPTURE-QR-001":"DEMO-QR-001";
            call(post("/api/visits/"+visit+"/scans"),visitor,Map.of("qrToken",foreign),409);
            JsonNode scan=call(post("/api/visits/"+visit+"/scans"),visitor,Map.of("qrToken",entries.get(0).get("token").asText()),201);
            assertThat(scan.get("participation").get("content").size()).isPositive();
            assertThat(scan.get("next").size()).isEqualTo(1);
            assertThat(scan.get("next").get(0).get("participationId").asLong()).isEqualTo(entries.get(1).get("participationId").asLong());
            JsonNode route=call(post("/api/visits/"+visit+"/routes"),visitor,Map.of("criterion","KOMBINACIJA"),201);
            assertThat(route.get("items").size()).isEqualTo(2);
            assertThat(route.get("items").get(0).get("participationId").asLong()).isEqualTo(entries.get(1).get("participationId").asLong());
        }
    }
}
