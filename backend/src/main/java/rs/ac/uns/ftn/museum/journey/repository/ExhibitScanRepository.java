package rs.ac.uns.ftn.museum.journey.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.museum.journey.domain.*;

public interface ExhibitScanRepository extends JpaRepository<ExhibitScan,Long> {
    List<ExhibitScan> findByVisitIdOrderByScannedAtAscIdAsc(Long id);
}
