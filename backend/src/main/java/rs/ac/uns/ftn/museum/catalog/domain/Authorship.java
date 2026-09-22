package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "autorstvo")
public class Authorship {
    @EmbeddedId private AuthorshipId id;
    @MapsId("authorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Author author;
    @MapsId("exhibitId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eksponat_id", nullable = false)
    private Exhibit exhibit;
    @Column(name = "vrsta_doprinosa", nullable = true, columnDefinition = "text")
    private String contributionType;
    @Column(name = "napomena", nullable = true, columnDefinition = "text")
    private String note;
    public AuthorshipId getId() { return id; }
    public void setId(AuthorshipId id) { this.id = id; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public Exhibit getExhibit() { return exhibit; }
    public void setExhibit(Exhibit exhibit) { this.exhibit = exhibit; }
    public String getContributionType() { return contributionType; }
    public void setContributionType(String contributionType) { this.contributionType = contributionType; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
