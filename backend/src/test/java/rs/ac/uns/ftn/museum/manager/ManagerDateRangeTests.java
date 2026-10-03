package rs.ac.uns.ftn.museum.manager;

import java.time.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ManagerDateRangeTests {
    @Test void springAndAutumnDaysFollowBelgradeDstRatherThanFixed24Hours(){
        ManagerAnalyticsRepository repository=mock(ManagerAnalyticsRepository.class);
        ManagerAnalyticsService service=new ManagerAnalyticsService(repository,Clock.systemUTC());
        service.dashboard(LocalDate.of(2026,3,29),LocalDate.of(2026,3,29));
        verify(repository).attendance(Instant.parse("2026-03-28T23:00:00Z"),Instant.parse("2026-03-29T22:00:00Z"));
        service.dashboard(LocalDate.of(2026,10,25),LocalDate.of(2026,10,25));
        verify(repository).attendance(Instant.parse("2026-10-24T22:00:00Z"),Instant.parse("2026-10-25T23:00:00Z"));
    }
}
