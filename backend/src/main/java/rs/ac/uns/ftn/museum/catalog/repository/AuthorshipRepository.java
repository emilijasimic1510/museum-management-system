package rs.ac.uns.ftn.museum.catalog.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public interface AuthorshipRepository extends JpaRepository<Authorship, AuthorshipId> {

}
