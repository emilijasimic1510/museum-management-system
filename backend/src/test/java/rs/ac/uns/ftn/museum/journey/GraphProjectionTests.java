package rs.ac.uns.ftn.museum.journey;

import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import rs.ac.uns.ftn.museum.journey.service.*;
import rs.ac.uns.ftn.museum.journey.domain.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphProjectionTests {
    @Test void realNeo4jProjectionIsIdempotentAndRanksAnonymousCatalog() {
        try(GenericContainer<?> neo4j=new GenericContainer<>("neo4j:5.26.0-community")
                .withEnv("NEO4J_AUTH","none")
                .withEnv("NEO4J_server_memory_heap_initial__size","256m")
                .withEnv("NEO4J_server_memory_heap_max__size","512m")
                .withExposedPorts(7687).waitingFor(Wait.forListeningPort()).withStartupTimeout(Duration.ofMinutes(2))) {
            neo4j.start();
            try(Driver driver=GraphDatabase.driver("bolt://"+neo4j.getHost()+":"+neo4j.getMappedPort(7687),AuthTokens.none())) {
                JdbcTemplate jdbc=mock(JdbcTemplate.class);
                when(jdbc.queryForList(contains("from eksponat"))).thenReturn(List.of(
                    Map.of("id",1L,"naziv","First","tema","Slika","period","1900","ucesce",10L,"izlozba",100L,"redosled",1),
                    Map.of("id",2L,"naziv","Second","tema","Slika","period","1900","ucesce",20L,"izlozba",100L,"redosled",2)));
                when(jdbc.queryForList(contains("from autorstvo"))).thenReturn(List.of(Map.of("autor",7L,"eksponat",1L),Map.of("autor",7L,"eksponat",2L)));
                when(jdbc.queryForList(contains("lag("))).thenReturn(List.of(Map.of("prethodni",1L,"naredni",2L,"broj",3L)));
                GraphProjection graph=new GraphProjection(driver,jdbc);
                graph.synchronize();graph.synchronize();
                assertThat(graph.scores(1L,List.of(2L),List.of("Slika")).get(2L)).isEqualTo(12d);
                assertThat(graph.scores(1L,List.of(2L),List.of("Slika"),RouteCriterion.INTERESOVANJA).get(2L)).isEqualTo(5d);
                assertThat(graph.scores(1L,List.of(2L),List.of("Slika"),RouteCriterion.POPULARNOST).get(2L)).isEqualTo(3d);
                try(Session session=driver.session()) {
                    assertThat(session.run("MATCH (n:Eksponat) RETURN count(n) AS count").single().get("count").asInt()).isEqualTo(2);
                    assertThat(session.run("MATCH ()-[r:CESTO_SLEDI]->() RETURN r.broj AS count").single().get("count").asLong()).isEqualTo(3);
                    assertThat(session.run("MATCH (n) WHERE n.email IS NOT NULL OR n.posetilacId IS NOT NULL OR n.lozinka_hash IS NOT NULL RETURN count(n) AS count").single().get("count").asInt()).isZero();
                }
            }
        }
    }
    @Test void graphFailureReturnsFallbackSignal() {
        Driver driver=mock(Driver.class);when(driver.session()).thenThrow(new IllegalStateException("Unavailable"));
        assertThat(new GraphProjection(driver,mock(JdbcTemplate.class)).scores(null,List.of(1L),List.of())).isEmpty();
    }
    @Test void demoPaymentsAreDisabledOutsideLocalAndTestProfiles() {
        MockEnvironment environment=new MockEnvironment();environment.setActiveProfiles("production");
        DemoPaymentGateway gateway=new DemoPaymentGateway(environment);
        assertThatThrownBy(()->gateway.charge(false)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        environment.setActiveProfiles("local");
        assertThat(gateway.charge(false).status()).isEqualTo(PaymentStatus.USPESNA);
        assertThat(gateway.charge(true).status()).isEqualTo(PaymentStatus.NEUSPESNA);
    }
}
