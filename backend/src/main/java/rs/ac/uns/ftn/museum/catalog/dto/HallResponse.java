package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import rs.ac.uns.ftn.museum.catalog.domain.*;

public record HallResponse(
        Long id,
        Long museumId,
        String name,
        BigDecimal area,
        Integer floor,
        Integer capacity) {}
