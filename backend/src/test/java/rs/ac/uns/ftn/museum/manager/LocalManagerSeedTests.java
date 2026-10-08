package rs.ac.uns.ftn.museum.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.ac.uns.ftn.museum.config.DemoDataSeeder;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles({"test","local"})
class LocalManagerSeedTests extends PostgresTestBase {
    @Autowired DemoDataSeeder seed;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;
    @Test void managerHasRoleEmployeeSubtypeAndStableBcryptHash(){
        String email="ivan.nikolic@museum.local";seed.seed();
        var before=jdbc.queryForMap("select a.id,a.lozinka_hash,z.id employee,m.nivo_izvestavanja from korisnicki_nalog a join zaposleni z on z.nalog_id=a.id join menadzer m on m.zaposleni_id=z.id where a.email=? and z.tip_zaposlenog='MANAGER'",email);
        seed.seed();assertThat(jdbc.queryForMap("select a.id,a.lozinka_hash,z.id employee,m.nivo_izvestavanja from korisnicki_nalog a join zaposleni z on z.nalog_id=a.id join menadzer m on m.zaposleni_id=z.id where a.email=?",email)).isEqualTo(before);
        assertThat(encoder.matches(DemoDataSeeder.PASSWORD,(String)before.get("lozinka_hash"))).isTrue();
        assertThat(before.get("nivo_izvestavanja")).isEqualTo("Svi muzeji — zbirni operativni izveštaji");
        assertThat(jdbc.queryForList("select r.naziv from nalog_uloga ar join uloga r on r.id=ar.uloga_id join korisnicki_nalog a on a.id=ar.nalog_id where a.email=?",String.class,email)).containsExactly("MANAGER");
    }
}
