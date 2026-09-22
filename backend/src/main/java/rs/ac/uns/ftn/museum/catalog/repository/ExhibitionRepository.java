package rs.ac.uns.ftn.museum.catalog.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exhibition e where e.id = :id")
    Optional<Exhibition> findLockedById(@Param("id") Long id);

    @Query("select count(e) > 0 from Exhibition e where e.hall.id = :hallId and e.status <> :cancelled and (:excludedId is null or e.id <> :excludedId) and e.start < :end and e.end > :start")
    boolean overlaps(@Param("hallId") Long hallId, @Param("excludedId") Long excludedId,
            @Param("start") Instant start, @Param("end") Instant end,
            @Param("cancelled") ExhibitionStatus cancelled);
}
