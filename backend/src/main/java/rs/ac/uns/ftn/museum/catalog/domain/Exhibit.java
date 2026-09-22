package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "eksponat")
public class Exhibit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "naziv", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "godina_nastanka", nullable = true, columnDefinition = "text")
    private String creationPeriod;

    @Column(name = "tip", nullable = true, columnDefinition = "text")
    private String type;

    @Column(name = "materijal", nullable = true, columnDefinition = "text")
    private String material;

    @Column(name = "poreklo", nullable = true, columnDefinition = "text")
    private String origin;

    @Column(name = "opis", nullable = true, columnDefinition = "text")
    private String description;

    @Column(name = "inventarski_broj", nullable = false, columnDefinition = "text", unique = true)
    private String inventoryNumber;

    public Exhibit() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreationPeriod() { return creationPeriod; }
    public void setCreationPeriod(String creationPeriod) { this.creationPeriod = creationPeriod; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInventoryNumber() { return inventoryNumber; }
    public void setInventoryNumber(String inventoryNumber) { this.inventoryNumber = inventoryNumber; }
}
