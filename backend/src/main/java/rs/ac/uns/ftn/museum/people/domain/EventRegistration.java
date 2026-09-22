package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "prijava_na_dogadjaj")
public class EventRegistration {
    @EmbeddedId private EventRegistrationId id;
    @MapsId("visitorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posetilac_id", nullable = false)
    private Visitor visitor;
    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Event event;
    @Column(name = "datum_prijave", nullable = false)
    private Instant registeredAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RegistrationStatus status;
    public EventRegistrationId getId() { return id; }
    public void setId(EventRegistrationId value) { this.id = value; }
    public Visitor getVisitor() { return visitor; }
    public void setVisitor(Visitor value) { this.visitor = value; }
    public Event getEvent() { return event; }
    public void setEvent(Event value) { this.event = value; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant value) { this.registeredAt = value; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus value) { this.status = value; }
}
