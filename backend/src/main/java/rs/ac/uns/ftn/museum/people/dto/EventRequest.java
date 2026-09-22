package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record EventRequest(
        @NotNull Long exhibitionId,
        Long guideId,
        @NotBlank String name,
        @NotNull EventType type,
        @NotNull Instant start,
        @NotNull Instant end,
        @NotNull @Min(0) Integer capacity,
        String language,
        @NotNull EventStatus status) {}
