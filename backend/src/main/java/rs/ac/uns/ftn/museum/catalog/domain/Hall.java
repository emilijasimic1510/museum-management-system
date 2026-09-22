package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sala")
public class Hall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "muzej_id", nullable = false)
    private Museum museum;

    @Column(name = "naziv", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "povrsina", nullable = true, precision = 19, scale = 2)
    private BigDecimal area;

    @Column(name = "sprat", nullable = true)
    private Integer floor;

    @Column(name = "kapacitet", nullable = true)
    private Integer capacity;

    public Hall() {}

    public Long getId() { return id; }
    public Museum getMuseum() { return museum; }
    public void setMuseum(Museum museum) { this.museum = museum; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
