package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "korisnicki_nalog")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "email", nullable = false, columnDefinition = "text", unique = true)
    private String email;
    @Column(name = "lozinka_hash", nullable = false, columnDefinition = "text")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;
    @Column(name = "aktivan", nullable = false)
    private Boolean active;
    @Column(name = "datum_registracije", nullable = false)
    private Instant registeredAt;
    @Column(name = "poslednja_prijava", nullable = true)
    private Instant lastLogin;
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
    public Instant getLastLogin() { return lastLogin; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
}
