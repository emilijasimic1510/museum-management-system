package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ucesce_eksponata_na_izlozbi")
public class ExhibitParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ucesca")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "izlozba_id", nullable = false)
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eksponat_id", nullable = false)
    private Exhibit exhibit;

    @Column(name = "datum_postavljanja", nullable = true)
    private Instant installedAt;

    @Column(name = "datum_uklanjanja", nullable = true)
    private Instant removedAt;

    @Column(name = "pozicija", nullable = true, columnDefinition = "text")
    private String position;

    @Column(name = "redosled_obilaska", nullable = true)
    private Integer tourOrder;

    @Column(name = "kontekstualni_opis", nullable = true, columnDefinition = "text")
    private String contextDescription;

    @Column(name = "qr_token", nullable = false, columnDefinition = "text", unique = true)
    private String qrToken;

    @Column(name = "qr_putanja", nullable = true, columnDefinition = "text")
    private String qrPath;

    public ExhibitParticipation() {}

    public Long getId() { return id; }
    public Exhibition getExhibition() { return exhibition; }
    public void setExhibition(Exhibition exhibition) { this.exhibition = exhibition; }
    public Exhibit getExhibit() { return exhibit; }
    public void setExhibit(Exhibit exhibit) { this.exhibit = exhibit; }
    public Instant getInstalledAt() { return installedAt; }
    public void setInstalledAt(Instant installedAt) { this.installedAt = installedAt; }
    public Instant getRemovedAt() { return removedAt; }
    public void setRemovedAt(Instant removedAt) { this.removedAt = removedAt; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public Integer getTourOrder() { return tourOrder; }
    public void setTourOrder(Integer tourOrder) { this.tourOrder = tourOrder; }
    public String getContextDescription() { return contextDescription; }
    public void setContextDescription(String contextDescription) { this.contextDescription = contextDescription; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public String getQrPath() { return qrPath; }
    public void setQrPath(String qrPath) { this.qrPath = qrPath; }
}
