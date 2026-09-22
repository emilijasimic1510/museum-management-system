package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;

@Entity
@Table(name="poseta")
public class Visit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="ulaznica_id", nullable=false, unique=true)
    private Ticket ticket;
    @Column(name = "vreme_ulaska", nullable = false)
    private Instant enteredAt;
    @Column(name = "vreme_izlaska", nullable = true)
    private Instant exitedAt;
    @Enumerated(EnumType.STRING) @Column(name="status", nullable=false, length=32)
    private VisitStatus status;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket value) { ticket = value; }
    public Instant getEnteredAt() { return enteredAt; }
    public void setEnteredAt(Instant value) { enteredAt = value; }
    public Instant getExitedAt() { return exitedAt; }
    public void setExitedAt(Instant value) { exitedAt = value; }
    public VisitStatus getStatus() { return status; }
    public void setStatus(VisitStatus value) { status = value; }
}
