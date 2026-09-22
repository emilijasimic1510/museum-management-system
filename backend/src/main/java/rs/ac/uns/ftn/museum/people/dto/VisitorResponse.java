package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record VisitorResponse(
        Long id,
        Long accountId,
        String firstName,
        String lastName,
        String phone,
        Boolean recommendationsConsent) {}
