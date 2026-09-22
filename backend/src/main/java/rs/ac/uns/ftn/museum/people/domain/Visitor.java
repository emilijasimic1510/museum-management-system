package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "posetilac")
public class Visitor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nalog_id", nullable = false, unique = true)
    private Account account;
    @Column(name = "ime", nullable = false, columnDefinition = "text")
    private String firstName;
    @Column(name = "prezime", nullable = false, columnDefinition = "text")
    private String lastName;
    @Column(name = "telefon", nullable = true, columnDefinition = "text")
    private String phone;
    @Column(name = "saglasnost_za_preporuke", nullable = false)
    private Boolean recommendationsConsent;
    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getRecommendationsConsent() { return recommendationsConsent; }
    public void setRecommendationsConsent(Boolean recommendationsConsent) { this.recommendationsConsent = recommendationsConsent; }
}
