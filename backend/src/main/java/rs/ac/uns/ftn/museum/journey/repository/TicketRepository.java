package rs.ac.uns.ftn.museum.journey.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.museum.journey.domain.*;

public interface TicketRepository extends JpaRepository<Ticket,Long> {
    List<Ticket> findByVisitorAccountIdOrderByIdDesc(Long id);
    Optional<Ticket> findByQrToken(String token);
    long countByTermIdAndStatusNot(Long id, TicketStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from Ticket t where t.id=:id")
    Optional<Ticket> lock(@Param("id") Long id);
}
