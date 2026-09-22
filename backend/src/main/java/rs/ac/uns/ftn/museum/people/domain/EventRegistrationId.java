package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Embeddable
public class EventRegistrationId implements java.io.Serializable {
    @Column(name = "posetilac_id", nullable = false) private Long visitorId;
    @Column(name = "dogadjaj_id", nullable = false) private Long eventId;
    public EventRegistrationId() {}
    public EventRegistrationId(Long visitorId, Long eventId) { this.visitorId = visitorId; this.eventId = eventId; }
    public Long getVisitorId() { return visitorId; }
    public Long getEventId() { return eventId; }
    @Override public boolean equals(Object other) { return other instanceof EventRegistrationId that && java.util.Objects.equals(visitorId, that.visitorId) && java.util.Objects.equals(eventId, that.eventId); }
    @Override public int hashCode() { return java.util.Objects.hash(visitorId, eventId); }
}
