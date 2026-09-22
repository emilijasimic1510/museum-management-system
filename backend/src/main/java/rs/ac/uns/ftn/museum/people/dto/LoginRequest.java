package rs.ac.uns.ftn.museum.people.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import rs.ac.uns.ftn.museum.people.domain.*;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
    @Override public String toString() { return "LoginRequest[credentials redacted]"; }
}
