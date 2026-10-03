package rs.ac.uns.ftn.museum.people.dto;

import java.time.Instant;
import rs.ac.uns.ftn.museum.people.domain.RegistrationStatus;

/** Guide-only attendance view; account credentials and contact details are excluded. */
public record ManagedRegistrationResponse(Long visitorId, Long eventId, Instant registeredAt,
        RegistrationStatus status, String firstName, String lastName) {}
