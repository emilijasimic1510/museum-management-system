package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record EmployeeResponse(
        Long id,
        Long accountId,
        Long museumId,
        String firstName,
        String lastName,
        String phone,
        LocalDate hiredOn,
        Long supervisorId,
        EmployeeType type,
        String specialization,
        String primaryLanguage,
        String counterLabel,
        String reportingLevel,
        String administrationLevel) {}
