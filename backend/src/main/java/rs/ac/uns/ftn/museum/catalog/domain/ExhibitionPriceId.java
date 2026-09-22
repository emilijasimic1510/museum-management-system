package rs.ac.uns.ftn.museum.catalog.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class ExhibitionPriceId implements Serializable {
    @Column(name = "izlozba_id", nullable = false)
    private Long exhibitionId;
    @Column(name = "datum_vazenja_od", nullable = false)
    private Instant validFrom;
    @Column(name = "kategorija", nullable = false, columnDefinition = "text")
    private String category;
    public ExhibitionPriceId() {}
    public ExhibitionPriceId(Long exhibitionId, Instant validFrom, String category) {
        this.exhibitionId = exhibitionId;
        this.validFrom = validFrom;
        this.category = category;
    }
    public Long getExhibitionId() { return exhibitionId; }
    public Instant getValidFrom() { return validFrom; }
    public String getCategory() { return category; }
    @Override public boolean equals(Object other) {
        if (!(other instanceof ExhibitionPriceId that)) return false;
        return Objects.equals(exhibitionId, that.exhibitionId) && Objects.equals(validFrom, that.validFrom) && Objects.equals(category, that.category);
    }
    @Override public int hashCode() { return Objects.hash(exhibitionId, validFrom, category); }
}
