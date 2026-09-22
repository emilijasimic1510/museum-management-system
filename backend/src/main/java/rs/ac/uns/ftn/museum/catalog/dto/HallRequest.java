package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public record HallRequest(
        @NotNull Long museumId,
        @NotBlank String name,
        BigDecimal area,
        Integer floor,
        Integer capacity) {}
