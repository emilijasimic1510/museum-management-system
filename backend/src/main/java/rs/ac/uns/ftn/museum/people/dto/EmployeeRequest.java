package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record EmployeeRequest(
        @NotNull Long accountId,
        @NotNull Long museumId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotNull LocalDate hiredOn,
        Long supervisorId,
        @NotNull EmployeeType type,
        String specialization,
        String primaryLanguage,
        String counterLabel,
        String reportingLevel,
        String administrationLevel) {}
