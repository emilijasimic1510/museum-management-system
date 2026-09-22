package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cenovnik_izlozbe")
public class ExhibitionPrice {
    @EmbeddedId private ExhibitionPriceId id;
    @MapsId("exhibitionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "izlozba_id", nullable = false)
    private Exhibition exhibition;
    @Column(name = "datum_vazenja_do", nullable = true)
    private Instant validTo;
    @Column(name = "cena", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @Column(name = "napomena", nullable = true, columnDefinition = "text")
    private String note;
    public ExhibitionPriceId getId() { return id; }
    public void setId(ExhibitionPriceId id) { this.id = id; }
    public Exhibition getExhibition() { return exhibition; }
    public void setExhibition(Exhibition exhibition) { this.exhibition = exhibition; }
    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
