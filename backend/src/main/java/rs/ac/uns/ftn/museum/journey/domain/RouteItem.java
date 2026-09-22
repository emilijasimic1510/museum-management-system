package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.people.domain.Visitor;

@Entity
@Table(name="stavka_rute")
public class RouteItem {
    @EmbeddedId
    private RouteItemId id;
    @MapsId("routeId") @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="ruta_id", nullable=false)
    private RecommendedRoute route;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="ucesce_id", nullable=false, unique=false)
    private ExhibitParticipation participation;
    @Column(name = "obrazlozenje", nullable = false, columnDefinition="text")
    private String explanation;
    @Column(name = "posecena", nullable = false)
    private Boolean visited;
    public RouteItemId getId() { return id; }
    public void setId(RouteItemId value) { id = value; }
    public RecommendedRoute getRoute() { return route; }
    public void setRoute(RecommendedRoute value) { route = value; }
    public ExhibitParticipation getParticipation() { return participation; }
    public void setParticipation(ExhibitParticipation value) { participation = value; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String value) { explanation = value; }
    public Boolean getVisited() { return visited; }
    public void setVisited(Boolean value) { visited = value; }
}
