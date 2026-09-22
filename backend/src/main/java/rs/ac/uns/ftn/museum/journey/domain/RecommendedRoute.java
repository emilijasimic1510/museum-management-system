package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;

@Entity
@Table(name="preporucena_ruta")
public class RecommendedRoute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="poseta_id", nullable=false, unique=false)
    private Visit visit;
    @Column(name = "datum_generisanja", nullable = false)
    private Instant generatedAt;
    @Enumerated(EnumType.STRING) @Column(name="kriterijum", nullable=false, length=32)
    private RouteCriterion criterion;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false, length=32)
    private VisitStatus status;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Visit getVisit() { return visit; }
    public void setVisit(Visit value) { visit = value; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant value) { generatedAt = value; }
    public RouteCriterion getCriterion() { return criterion; }
    public void setCriterion(RouteCriterion value) { criterion = value; }
    public VisitStatus getStatus() { return status; }
    public void setStatus(VisitStatus value) { status = value; }
}
