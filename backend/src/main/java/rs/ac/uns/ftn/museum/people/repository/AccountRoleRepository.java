package rs.ac.uns.ftn.museum.people.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.museum.people.domain.*;

public interface AccountRoleRepository extends JpaRepository<AccountRole, AccountRoleId> {
    @Query("select ar.role.name from AccountRole ar where ar.account.id = :id")
    List<RoleName> roleNames(@Param("id") Long id);
    List<AccountRole> findByAccountId(Long id);
    long countByAccountId(Long id);
}
