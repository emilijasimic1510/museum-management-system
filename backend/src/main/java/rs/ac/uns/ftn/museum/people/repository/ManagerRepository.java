package rs.ac.uns.ftn.museum.people.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.museum.people.domain.*;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

}
