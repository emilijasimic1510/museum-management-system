package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "zaposleni")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tip_zaposlenog", length = 32)
public abstract class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nalog_id", nullable = false, unique = true)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "muzej_id", nullable = false)
    private Museum museum;
    @Column(name = "ime", nullable = false, columnDefinition = "text")
    private String firstName;
    @Column(name = "prezime", nullable = false, columnDefinition = "text")
    private String lastName;
    @Column(name = "telefon", nullable = true, columnDefinition = "text")
    private String phone;
    @Column(name = "datum_zaposlenja", nullable = false)
    private LocalDate hiredOn;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "nadredjeni_id", nullable = true)
    private Employee supervisor;
    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public Museum getMuseum() { return museum; }
    public void setMuseum(Museum museum) { this.museum = museum; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getHiredOn() { return hiredOn; }
    public void setHiredOn(LocalDate hiredOn) { this.hiredOn = hiredOn; }
    public Employee getSupervisor() { return supervisor; }
    public void setSupervisor(Employee supervisor) { this.supervisor = supervisor; }
    public abstract EmployeeType getType();
}
