package rs.ac.uns.ftn.museum;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MuseumBackendApplicationTests extends rs.ac.uns.ftn.museum.support.PostgresTestBase {
    @Test
    void contextLoads() {
    }
}
