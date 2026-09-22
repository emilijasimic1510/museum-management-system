package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public record ExhibitionResponse(
        Long id,
        Long hallId,
        String name,
        String description,
        Instant start,
        Instant end,
        ExhibitionStatus status,
        Boolean onlineVisible,
        List<TermResponse> terms) {}
