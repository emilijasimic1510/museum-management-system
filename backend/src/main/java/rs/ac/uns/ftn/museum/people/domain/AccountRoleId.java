package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Embeddable
public class AccountRoleId implements java.io.Serializable {
    @Column(name = "nalog_id", nullable = false) private Long accountId;
    @Column(name = "uloga_id", nullable = false) private Long roleId;
    public AccountRoleId() {}
    public AccountRoleId(Long accountId, Long roleId) { this.accountId = accountId; this.roleId = roleId; }
    public Long getAccountId() { return accountId; }
    public Long getRoleId() { return roleId; }
    @Override public boolean equals(Object other) { return other instanceof AccountRoleId that && java.util.Objects.equals(accountId, that.accountId) && java.util.Objects.equals(roleId, that.roleId); }
    @Override public int hashCode() { return java.util.Objects.hash(accountId, roleId); }
}
