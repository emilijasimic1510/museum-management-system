package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record EventResponse(
        Long id,
        Long exhibitionId,
        Long guideId,
        String name,
        EventType type,
        Instant start,
        Instant end,
        Integer capacity,
        String language,
        EventStatus status,
        long registeredCount) {}
