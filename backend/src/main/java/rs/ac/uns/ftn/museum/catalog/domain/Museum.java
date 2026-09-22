package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "muzej")
public class Museum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "naziv", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "godina_osnivanja", nullable = true)
    private Integer foundationYear;

    @Column(name = "adresa", nullable = false, columnDefinition = "text")
    private String address;

    @Column(name = "opis", nullable = true, columnDefinition = "text")
    private String description;

    @Column(name = "aktivan", nullable = false)
    private Boolean active;

    public Museum() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getFoundationYear() { return foundationYear; }
    public void setFoundationYear(Integer foundationYear) { this.foundationYear = foundationYear; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
