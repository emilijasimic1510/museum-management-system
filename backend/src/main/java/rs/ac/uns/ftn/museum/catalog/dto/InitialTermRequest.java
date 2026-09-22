package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public record InitialTermRequest(
        @NotNull Instant start,
        @NotNull Instant end,
        @NotNull @Min(0) Integer capacity,
        @NotNull TermStatus status) {}
