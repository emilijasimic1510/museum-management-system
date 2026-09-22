package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Embeddable
public class VisitorInterestId implements java.io.Serializable {
    @Column(name = "posetilac_id", nullable = false) private Long visitorId;
    @Column(name = "interesovanje_id", nullable = false) private Long interestId;
    public VisitorInterestId() {}
    public VisitorInterestId(Long visitorId, Long interestId) { this.visitorId = visitorId; this.interestId = interestId; }
    public Long getVisitorId() { return visitorId; }
    public Long getInterestId() { return interestId; }
    @Override public boolean equals(Object other) { return other instanceof VisitorInterestId that && java.util.Objects.equals(visitorId, that.visitorId) && java.util.Objects.equals(interestId, that.interestId); }
    @Override public int hashCode() { return java.util.Objects.hash(visitorId, interestId); }
}
