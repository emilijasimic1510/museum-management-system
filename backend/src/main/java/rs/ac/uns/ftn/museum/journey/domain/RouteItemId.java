package rs.ac.uns.ftn.museum.journey.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RouteItemId implements Serializable {
    @Column(name="ruta_id") private Long routeId;
    @Column(name="redni_broj") private Integer position;
    public RouteItemId() {}
    public RouteItemId(Long routeId, Integer position) { this.routeId=routeId; this.position=position; }
    public Long getRouteId() { return routeId; }
    public Integer getPosition() { return position; }
    @Override public boolean equals(Object o) { return o instanceof RouteItemId other && Objects.equals(routeId,other.routeId) && Objects.equals(position,other.position); }
    @Override public int hashCode() { return Objects.hash(routeId,position); }
}
