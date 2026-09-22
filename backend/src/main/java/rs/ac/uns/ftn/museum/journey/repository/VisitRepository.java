package rs.ac.uns.ftn.museum.journey.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.museum.journey.domain.*;

public interface VisitRepository extends JpaRepository<Visit,Long> {
    List<Visit> findByTicketVisitorAccountIdOrderByIdDesc(Long id);
    boolean existsByTicketId(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select v from Visit v where v.id=:id")
    Optional<Visit> lock(@Param("id") Long id);
}
