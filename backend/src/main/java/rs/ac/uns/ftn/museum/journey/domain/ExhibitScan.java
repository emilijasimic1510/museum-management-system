package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;

@Entity
@Table(name="skeniranje_eksponata")
public class ExhibitScan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="poseta_id", nullable=false, unique=false)
    private Visit visit;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="ucesce_id", nullable=false, unique=false)
    private ExhibitParticipation participation;
    @Column(name = "vreme_skeniranja", nullable = false)
    private Instant scannedAt;
    @Column(name = "trajanje_pregleda", nullable = true)
    private Integer durationSeconds;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Visit getVisit() { return visit; }
    public void setVisit(Visit value) { visit = value; }
    public ExhibitParticipation getParticipation() { return participation; }
    public void setParticipation(ExhibitParticipation value) { participation = value; }
    public Instant getScannedAt() { return scannedAt; }
    public void setScannedAt(Instant value) { scannedAt = value; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer value) { durationSeconds = value; }
}
