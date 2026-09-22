package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record VisitorProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotNull Boolean recommendationsConsent) {}
