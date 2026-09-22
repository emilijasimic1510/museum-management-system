package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "autor")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "ime", nullable = false, columnDefinition = "text")
    private String firstName;

    @Column(name = "prezime", nullable = true, columnDefinition = "text")
    private String lastName;

    @Column(name = "godina_rodjenja", nullable = true)
    private Integer birthYear;

    @Column(name = "godina_smrti", nullable = true)
    private Integer deathYear;

    @Column(name = "biografija", nullable = true, columnDefinition = "text")
    private String biography;

    public Author() {}

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
    public Integer getDeathYear() { return deathYear; }
    public void setDeathYear(Integer deathYear) { this.deathYear = deathYear; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
}
