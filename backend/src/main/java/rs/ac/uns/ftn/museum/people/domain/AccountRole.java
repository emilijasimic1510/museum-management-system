package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "nalog_uloga")
public class AccountRole {
    @EmbeddedId private AccountRoleId id;
    @MapsId("accountId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nalog_id", nullable = false)
    private Account account;
    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uloga_id", nullable = false)
    private Role role;
    @Column(name = "datum_dodele", nullable = false)
    private Instant assignedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dodelio_id")
    private Account assignedBy;
    public AccountRoleId getId() { return id; }
    public void setId(AccountRoleId value) { this.id = value; }
    public Account getAccount() { return account; }
    public void setAccount(Account value) { this.account = value; }
    public Role getRole() { return role; }
    public void setRole(Role value) { this.role = value; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant value) { this.assignedAt = value; }
    public Account getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Account value) { this.assignedBy = value; }
}
