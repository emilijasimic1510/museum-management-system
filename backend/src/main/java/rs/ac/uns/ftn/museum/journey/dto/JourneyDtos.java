package rs.ac.uns.ftn.museum.journey.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import rs.ac.uns.ftn.museum.journey.domain.*;

public final class JourneyDtos {
    private JourneyDtos() {}
    public record PurchaseRequest(@NotNull Long termId, @NotBlank String category, Long visitorId,
            @NotNull PaymentMethod paymentMethod, boolean demoDecline) {}
    public record RetryPaymentRequest(@NotNull PaymentMethod paymentMethod, boolean demoDecline) {}
    public record ValidateTicketRequest(@NotBlank String qrToken, @NotNull Long termId) {
        @Override public String toString() { return "ValidateTicketRequest[token redacted]"; }
    }
    public record ScanRequest(@NotBlank String qrToken, @Min(0) Integer durationSeconds) {}
    public record RatingRequest(@NotNull @Min(1) @Max(5) Integer rating, String comment) {}
    public record RouteRequest(@NotNull RouteCriterion criterion) {}
    public record TicketResponse(Long id, Long visitorId, Long termId, Long exhibitionId, Instant priceValidFrom,
            String category, Instant purchasedAt, BigDecimal paidPrice, TicketStatus status, String qrToken, Instant validUntil) {
        @Override public String toString() { return "TicketResponse[id="+id+", token redacted]"; }
    }
    public record PaymentResponse(Long id, Long ticketId, BigDecimal amount, PaymentMethod method,
            PaymentStatus status, String externalReference, Instant createdAt) {}
    public record VisitResponse(Long id, Long ticketId, Instant enteredAt, Instant exitedAt, VisitStatus status,
            Long exhibitionId, String exhibitionName) {}
    public record ScanResponse(Long id, Long visitId, Long participationId, Instant scannedAt, Integer durationSeconds,
            List<Recommendation> next, ParticipationContent participation) {}
    public record RatingResponse(Long id, Long visitId, Integer rating, String comment, Instant ratedAt) {}
    public record Recommendation(Long participationId, Long exhibitId, String name, String explanation) {}
    public record RouteItemResponse(Integer position, Long participationId, String explanation, Boolean visited) {}
    public record RouteResponse(Long id, Long visitId, Instant generatedAt, RouteCriterion criterion,
            VisitStatus status, List<RouteItemResponse> items) {}
    public record PriceResponse(Long exhibitionId, Instant validFrom, Instant validTo, String category, BigDecimal price) {}
    public record ExhibitionRecommendation(Long exhibitionId,String name,Instant start,Instant end,String explanation) {}
    public record ContentResponse(Long id,String type,String path,String language,Integer version) {}
    public record ParticipationContent(Long participationId,String name,String contextDescription,List<ContentResponse> content) {}
    public record ContentAccess(Long participationId,Long exhibitionId,String exhibitionName,Long visitId,
            String state,ParticipationContent participation) {}
}
