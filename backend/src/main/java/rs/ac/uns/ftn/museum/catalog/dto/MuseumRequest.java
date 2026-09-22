package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public record MuseumRequest(
        @NotBlank String name,
        Integer foundationYear,
        @NotBlank String address,
        String description,
        @NotNull Boolean active) {}
