package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "izlozba")
public class Exhibition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Hall hall;

    @Column(name = "naziv", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "opis", nullable = true, columnDefinition = "text")
    private String description;

    @Column(name = "datum_pocetka", nullable = false)
    private Instant start;

    @Column(name = "datum_zavrsetka", nullable = false)
    private Instant end;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExhibitionStatus status;

    @Column(name = "online_vidljiva", nullable = false)
    private Boolean onlineVisible;

    public Exhibition() {}

    public Long getId() { return id; }
    public Hall getHall() { return hall; }
    public void setHall(Hall hall) { this.hall = hall; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }
    public ExhibitionStatus getStatus() { return status; }
    public void setStatus(ExhibitionStatus status) { this.status = status; }
    public Boolean getOnlineVisible() { return onlineVisible; }
    public void setOnlineVisible(Boolean onlineVisible) { this.onlineVisible = onlineVisible; }
}
