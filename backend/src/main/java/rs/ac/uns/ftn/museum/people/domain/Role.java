package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "uloga")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "naziv", nullable = false, length = 32, unique = true)
    private RoleName name;
    @Column(name = "opis", nullable = true, columnDefinition = "text")
    private String description;
    public Long getId() { return id; }
    public RoleName getName() { return name; }
    public void setName(RoleName name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
