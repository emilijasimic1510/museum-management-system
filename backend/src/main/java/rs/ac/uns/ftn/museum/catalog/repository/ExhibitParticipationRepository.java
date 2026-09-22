package rs.ac.uns.ftn.museum.catalog.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public interface ExhibitParticipationRepository extends JpaRepository<ExhibitParticipation, Long> {
    @EntityGraph(attributePaths = {"exhibit", "exhibition"})
    List<ExhibitParticipation> findByExhibitIdAndExhibitionOnlineVisibleTrueOrderById(Long exhibitId);
    @EntityGraph(attributePaths = {"exhibit", "exhibition"})
    List<ExhibitParticipation> findByExhibitionIdOrderById(Long exhibitionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ExhibitParticipation p where p.id = :id")
    Optional<ExhibitParticipation> findLockedById(@Param("id") Long id);
}
