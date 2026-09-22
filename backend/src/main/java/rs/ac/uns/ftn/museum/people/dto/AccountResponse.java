package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record AccountResponse(
        Long id,
        String email,
        Boolean active,
        Instant registeredAt,
        Instant lastLogin,
        List<RoleName> roles) {}
