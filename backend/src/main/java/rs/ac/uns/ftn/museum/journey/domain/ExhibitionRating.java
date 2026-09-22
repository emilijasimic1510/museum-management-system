package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;

@Entity
@Table(name="ocena_izlozbe")
public class ExhibitionRating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="poseta_id", nullable=false, unique=true)
    private Visit visit;
    @Column(name = "ocena", nullable = false)
    private Integer rating;
    @Column(name = "komentar", nullable = true, columnDefinition="text")
    private String comment;
    @Column(name = "datum_ocene", nullable = false)
    private Instant ratedAt;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Visit getVisit() { return visit; }
    public void setVisit(Visit value) { visit = value; }
    public Integer getRating() { return rating; }
    public void setRating(Integer value) { rating = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
    public Instant getRatedAt() { return ratedAt; }
    public void setRatedAt(Instant value) { ratedAt = value; }
}
