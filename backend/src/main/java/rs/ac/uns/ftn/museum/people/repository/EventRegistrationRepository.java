package rs.ac.uns.ftn.museum.people.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.museum.people.domain.*;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, EventRegistrationId> {
    long countByEventIdAndStatusNot(Long eventId, RegistrationStatus status);
    @EntityGraph(attributePaths = "visitor")
    List<EventRegistration> findByEventId(Long eventId);
    List<EventRegistration> findByVisitorId(Long visitorId);
}
