package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public record ExhibitionCreateRequest(
        @NotNull Long hallId,
        @NotBlank String name,
        String description,
        @NotNull Instant start,
        @NotNull Instant end,
        @NotNull ExhibitionStatus status,
        @NotNull Boolean onlineVisible,
        @NotEmpty List<@NotNull @Valid InitialTermRequest> terms) {}
