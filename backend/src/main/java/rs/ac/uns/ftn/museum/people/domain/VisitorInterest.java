package rs.ac.uns.ftn.museum.people.domain;

import jakarta.persistence.*;
import java.time.*;
import rs.ac.uns.ftn.museum.catalog.domain.Museum;
import rs.ac.uns.ftn.museum.catalog.domain.Exhibition;

@Entity
@Table(name = "posetilac_interesovanje")
public class VisitorInterest {
    @EmbeddedId private VisitorInterestId id;
    @MapsId("visitorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posetilac_id", nullable = false)
    private Visitor visitor;
    @MapsId("interestId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interesovanje_id", nullable = false)
    private Interest interest;
    @Column(name = "nivo", nullable = true)
    private Integer level;
    public VisitorInterestId getId() { return id; }
    public void setId(VisitorInterestId value) { this.id = value; }
    public Visitor getVisitor() { return visitor; }
    public void setVisitor(Visitor value) { this.visitor = value; }
    public Interest getInterest() { return interest; }
    public void setInterest(Interest value) { this.interest = value; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer value) { this.level = value; }
}
