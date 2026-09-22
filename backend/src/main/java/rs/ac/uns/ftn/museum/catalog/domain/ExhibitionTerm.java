package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "termin_izlozbe")
public class ExhibitionTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "izlozba_id", nullable = false)
    private Exhibition exhibition;

    @Column(name = "pocetak", nullable = false)
    private Instant start;

    @Column(name = "kraj", nullable = false)
    private Instant end;

    @Column(name = "kapacitet", nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TermStatus status;

    public ExhibitionTerm() {}

    public Long getId() { return id; }
    public Exhibition getExhibition() { return exhibition; }
    public void setExhibition(Exhibition exhibition) { this.exhibition = exhibition; }
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public TermStatus getStatus() { return status; }
    public void setStatus(TermStatus status) { this.status = status; }
}
