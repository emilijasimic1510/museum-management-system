package rs.ac.uns.ftn.museum.journey;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import rs.ac.uns.ftn.museum.journey.service.*;
import rs.ac.uns.ftn.museum.journey.controller.LocalDemoQrController;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class RecommendationAvailabilityTests {
 @Test void stalledDriverIsBounded() throws Exception {
  Driver driver=mock(Driver.class);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  when(driver.session()).thenAnswer(inv->{entered.countDown();while(release.getCount()>0){try{release.await();}catch(InterruptedException ignored){}}throw new IllegalStateException("offline");});
  GraphProjection graph=new GraphProjection(driver,mock(JdbcTemplate.class));
  try {var first=CompletableFuture.supplyAsync(()->graph.scores(null,List.of(1L),List.of()));assertThat(entered.await(1,TimeUnit.SECONDS)).isTrue();assertThat(graph.scores(null,List.of(2L),List.of())).isEmpty();assertThat(first.get(3,TimeUnit.SECONDS)).isEmpty();verify(driver,times(1)).session();}finally{release.countDown();graph.close();}
 }
 @Test void demoChecksAccessWithoutRecommendations() {VisitService visits=mock(VisitService.class);new LocalDemoQrController(mock(JdbcTemplate.class),visits).forVisit(3L);verify(visits).requireActiveOwner(3L);verify(visits,never()).next(anyLong());}
 @Test void deniedDemoDoesNotQueryCatalog() {VisitService visits=mock(VisitService.class);JdbcTemplate jdbc=mock(JdbcTemplate.class);doThrow(new org.springframework.security.access.AccessDeniedException("denied")).when(visits).requireActiveOwner(3L);assertThatThrownBy(()->new LocalDemoQrController(jdbc,visits).forVisit(3L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(jdbc);}
}
