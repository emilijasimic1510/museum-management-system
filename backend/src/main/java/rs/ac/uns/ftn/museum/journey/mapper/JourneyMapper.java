package rs.ac.uns.ftn.museum.journey.mapper;

import java.util.List;
import rs.ac.uns.ftn.museum.journey.domain.*;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;

public final class JourneyMapper {
    private JourneyMapper() {}
    public static TicketResponse ticket(Ticket t) {
        var p=t.getPrice().getId();
        return new TicketResponse(t.getId(),t.getVisitor()==null?null:t.getVisitor().getId(),t.getTerm().getId(),
                p.getExhibitionId(),p.getValidFrom(),p.getCategory(),t.getPurchasedAt(),t.getPaidPrice(),t.getStatus(),t.getQrToken(),t.getValidUntil());
    }
    public static PaymentResponse payment(Payment p) { return new PaymentResponse(p.getId(),p.getTicket().getId(),p.getAmount(),p.getMethod(),p.getStatus(),p.getExternalReference(),p.getCreatedAt()); }
    public static VisitResponse visit(Visit v) {
        var exhibition=v.getTicket().getTerm().getExhibition();
        return new VisitResponse(v.getId(),v.getTicket().getId(),v.getEnteredAt(),v.getExitedAt(),v.getStatus(),exhibition.getId(),exhibition.getName());
    }
    public static RatingResponse rating(ExhibitionRating r) { return new RatingResponse(r.getId(),r.getVisit().getId(),r.getRating(),r.getComment(),r.getRatedAt()); }
    public static RouteResponse route(RecommendedRoute r,List<RouteItem> items) {
        return new RouteResponse(r.getId(),r.getVisit().getId(),r.getGeneratedAt(),r.getCriterion(),r.getStatus(),
            items.stream().map(i->new RouteItemResponse(i.getId().getPosition(),i.getParticipation().getId(),i.getExplanation(),i.getVisited())).toList());
    }
}
