package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "digitalni_sadrzaj")
public class DigitalContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ucesce_id", nullable = false)
    private ExhibitParticipation participation;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_sadrzaja", nullable = false, length = 32)
    private ContentType type;

    @Column(name = "putanja", nullable = false, columnDefinition = "text")
    private String path;

    @Column(name = "jezik", nullable = true, columnDefinition = "text")
    private String language;

    @Column(name = "verzija", nullable = false)
    private Integer version;

    @Column(name = "generisan_automatski", nullable = false)
    private Boolean automaticallyGenerated;

    @Column(name = "datum_kreiranja", nullable = false)
    private Instant createdAt;

    public DigitalContent() {}

    public Long getId() { return id; }
    public ExhibitParticipation getParticipation() { return participation; }
    public void setParticipation(ExhibitParticipation participation) { this.participation = participation; }
    public ContentType getType() { return type; }
    public void setType(ContentType type) { this.type = type; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Boolean getAutomaticallyGenerated() { return automaticallyGenerated; }
    public void setAutomaticallyGenerated(Boolean automaticallyGenerated) { this.automaticallyGenerated = automaticallyGenerated; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
