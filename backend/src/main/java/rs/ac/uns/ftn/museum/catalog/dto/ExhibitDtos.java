package rs.ac.uns.ftn.museum.catalog.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import rs.ac.uns.ftn.museum.catalog.domain.ContentType;

public final class ExhibitDtos {
    private ExhibitDtos() {}
    public record ExhibitRequest(@NotBlank String name, String creationPeriod, String type, String material, String origin, String description, @NotBlank String inventoryNumber) {}
    public record ExhibitResponse(Long id, String name, String creationPeriod, String type, String material, String origin, String description, List<AuthorSummary> authors) {}
    public record ManagedExhibitResponse(ExhibitResponse exhibit, String inventoryNumber) {}
    public record AuthorRequest(@NotBlank String firstName, String lastName, Integer birthYear, Integer deathYear, String biography) {}
    public record AuthorResponse(Long id, String firstName, String lastName, Integer birthYear, Integer deathYear, String biography) {}
    public record AuthorSummary(Long id, String name, String contributionType) {}
    public record AuthorshipResponse(AuthorSummary author) {}
    public record CatalogReference(Long id, String name) {}
    public record ParticipationResponse(CatalogReference exhibition, CatalogReference exhibit, String contextDescription) {}
    public record ParticipationRequest(@NotNull @Positive Long exhibitionId, @NotNull @Positive Long exhibitId,
            Instant installedAt, Instant removedAt, String position, @Positive Integer tourOrder,
            String contextDescription, @Size(max=512) String qrToken, String qrPath) {
        @Override public String toString() { return "ParticipationRequest[QR data redacted]"; }
    }
    public record ManagedParticipationResponse(Long id, Long exhibitionId, Long exhibitId, Instant installedAt,
            Instant removedAt, String position, Integer tourOrder, String contextDescription, String qrToken, String qrPath) {
        @Override public String toString() { return "ManagedParticipationResponse[id="+id+", QR data redacted]"; }
    }
    public record DigitalContentRequest(@NotNull @Positive Long participationId, @NotNull ContentType type,
            @NotBlank String path, String language, @NotNull @Positive Integer version,
            @NotNull Boolean automaticallyGenerated) {
        @Override public String toString() { return "DigitalContentRequest[reference redacted]"; }
    }
    public record DigitalContentResponse(Long id, Long participationId, ContentType type, String path,
            String language, Integer version, Boolean automaticallyGenerated, Instant createdAt) {
        @Override public String toString() { return "DigitalContentResponse[id="+id+", reference redacted]"; }
    }
}
