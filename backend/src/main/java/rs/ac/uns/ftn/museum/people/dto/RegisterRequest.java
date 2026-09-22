package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min=12,max=72) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotNull Boolean recommendationsConsent) {
    @Override public String toString() { return "RegisterRequest[credentials redacted]"; }
}
