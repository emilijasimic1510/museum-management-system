package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "dogadjaj")
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "izlozba_id", nullable = false)
    private Exhibition exhibition;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "vodic_id", nullable = true)
    private Guide guide;
    @Column(name = "naziv", nullable = false, columnDefinition = "text")
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "tip", nullable = false, length = 32)
    private EventType type;
    @Column(name = "pocetak", nullable = false)
    private Instant start;
    @Column(name = "kraj", nullable = false)
    private Instant end;
    @Column(name = "kapacitet", nullable = false)
    private Integer capacity;
    @Column(name = "jezik", nullable = true, columnDefinition = "text")
    private String language;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EventStatus status;
    public Long getId() { return id; }
    public Exhibition getExhibition() { return exhibition; }
    public void setExhibition(Exhibition exhibition) { this.exhibition = exhibition; }
    public Guide getGuide() { return guide; }
    public void setGuide(Guide guide) { this.guide = guide; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
}
