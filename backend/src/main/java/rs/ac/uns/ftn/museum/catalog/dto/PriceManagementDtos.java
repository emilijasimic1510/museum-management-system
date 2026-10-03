package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class PriceManagementDtos {
    private PriceManagementDtos() {}
    public record Create(@NotBlank String category, @NotNull Instant validFrom, Instant validTo,
            @NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal price, String note) {}
    public record Update(Instant validTo,
            @NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal price, String note) {}
    public record Response(Long exhibitionId, String category, Instant validFrom, Instant validTo, BigDecimal price, String note) {}
}
