package rs.ac.uns.ftn.museum.people;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import rs.ac.uns.ftn.museum.config.DemoDataSeeder;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE) @ActiveProfiles({"test","local"})
class LocalAdminSeedTests extends PostgresTestBase {
    @Autowired DemoDataSeeder seed;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired MockMvc mvc;@Autowired ObjectMapper json;
    @Test void localAdministratorIsIdempotentHasOneSubtypeAndCanLogin() throws Exception {
        String email="marija.ilic@museum.local";seed.seed();
        String sql="select a.id,a.lozinka_hash,z.id employee,s.nivo_administracije from korisnicki_nalog a join zaposleni z on z.nalog_id=a.id join administrator s on s.zaposleni_id=z.id where a.email=? and z.tip_zaposlenog='ADMINISTRATOR'";
        var before=jdbc.queryForMap(sql,email);seed.seed();assertThat(jdbc.queryForMap(sql,email)).isEqualTo(before);
        assertThat(encoder.matches(DemoDataSeeder.PASSWORD,(String)before.get("lozinka_hash"))).isTrue();assertThat(before.get("nivo_administracije")).isEqualTo("Nalozi, zaposleni i muzejski program");
        assertThat(jdbc.queryForList("select r.naziv from nalog_uloga ar join uloga r on r.id=ar.uloga_id join korisnicki_nalog a on a.id=ar.nalog_id where a.email=?",String.class,email)).containsExactly("ADMINISTRATOR");
        for(String table:java.util.List.of("kustos","vodic","blagajnik","menadzer"))assertThat(jdbc.queryForObject("select count(*) from "+table+" where zaposleni_id=?",Long.class,before.get("employee"))).isZero();
        String login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("email",email,"password",DemoDataSeeder.PASSWORD)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=json.readTree(login).path("accessToken").asText();mvc.perform(get("/api/admin/accounts").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/manager/dashboard").param("from","2026-01-01").param("to","2026-12-31").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
    }
}
