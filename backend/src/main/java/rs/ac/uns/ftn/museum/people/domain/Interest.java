package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "interesovanje")
public class Interest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "naziv", nullable = false, columnDefinition = "text", unique = true)
    private String name;
    @Column(name = "opis", nullable = true, columnDefinition = "text")
    private String description;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
