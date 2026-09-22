package rs.ac.uns.ftn.museum.journey.service;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.people.security.CurrentUser;
import rs.ac.uns.ftn.museum.journey.domain.*;
import rs.ac.uns.ftn.museum.journey.repository.*;
import rs.ac.uns.ftn.museum.journey.mapper.JourneyMapper;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;

@Service
@Transactional
public class VisitService {
    private final EntityManager em;
    private final VisitRepository visits;
    private final ExhibitScanRepository scans;
    private final ExhibitionRatingRepository ratings;
    private final RecommendedRouteRepository routes;
    private final RouteItemRepository items;
    private final RecommendationService recommendations;
    private final Clock clock;
    public VisitService(EntityManager em,VisitRepository visits,ExhibitScanRepository scans,ExhibitionRatingRepository ratings,
            RecommendedRouteRepository routes,RouteItemRepository items,RecommendationService recommendations,Clock clock) {
        this.em=em;this.visits=visits;this.scans=scans;this.ratings=ratings;this.routes=routes;this.items=items;this.recommendations=recommendations;this.clock=clock;
    }
    @Transactional(readOnly=true) public List<VisitResponse> mine() {
        return visits.findByTicketVisitorAccountIdOrderByIdDesc(CurrentUser.id()).stream().map(JourneyMapper::visit).toList();
    }
    @Transactional(readOnly=true) public VisitResponse get(Long id) { return JourneyMapper.visit(own(id,false)); }
    public VisitResponse finish(Long id) {
        Visit visit=own(id,true);
        if(visit.getStatus()==VisitStatus.ZAVRSENA)return JourneyMapper.visit(visit);
        close(visit);return JourneyMapper.visit(visit);
    }
    public ScanResponse scan(Long id,ScanRequest request) {
        Visit visit=own(id,true);active(visit);
        List<ExhibitParticipation> matches=em.createQuery("select p from ExhibitParticipation p where p.qrToken=:token",ExhibitParticipation.class)
                .setParameter("token",request.qrToken()).getResultList();
        if(matches.isEmpty())throw new NotFoundException("Participation QR token not found");
        ExhibitParticipation participation=matches.getFirst();Instant now=clock.instant();
        if(!participation.getExhibition().getId().equals(visit.getTicket().getTerm().getExhibition().getId())
            || (participation.getInstalledAt()!=null && now.isBefore(participation.getInstalledAt()))
            || (participation.getRemovedAt()!=null && !now.isBefore(participation.getRemovedAt())))
            throw new ConflictException("Participation is unavailable in this exhibition");
        ExhibitScan scan=new ExhibitScan();scan.setVisit(visit);scan.setParticipation(participation);scan.setScannedAt(now);scan.setDurationSeconds(request.durationSeconds());
        scans.saveAndFlush(scan);
        for(RecommendedRoute route:routes.findByVisitIdOrderByIdDesc(id)) {
            List<RouteItem> routeItems=items.findByRouteIdOrderByIdPosition(route.getId());
            routeItems.stream().filter(i->i.getParticipation().getId().equals(participation.getId())).forEach(i->i.setVisited(true));
            if(routeItems.stream().allMatch(i->Boolean.TRUE.equals(i.getVisited())))route.setStatus(VisitStatus.ZAVRSENA);
        }
        return scanDto(scan,recommendations.recommend(visit,RouteCriterion.KOMBINACIJA).stream().limit(1).toList());
    }
    @Transactional(readOnly=true) public List<ScanResponse> scans(Long id) {
        own(id,false);return scans.findByVisitIdOrderByScannedAtAscIdAsc(id).stream().map(s->scanDto(s,List.of())).toList();
    }
    public RatingResponse rate(Long id,RatingRequest request) {
        Visit visit=own(id,true);
        if(visit.getStatus()!=VisitStatus.ZAVRSENA) {
            if(!clock.instant().isBefore(visit.getTicket().getValidUntil()) || !clock.instant().isBefore(visit.getTicket().getTerm().getEnd())) close(visit);
            else throw new ConflictException("Rating requires a finished visit");
        }
        if(ratings.findByVisitId(id).isPresent())throw new ConflictException("Visit already has a rating");
        ExhibitionRating rating=new ExhibitionRating();rating.setVisit(visit);rating.setRating(request.rating());rating.setComment(request.comment());rating.setRatedAt(clock.instant());
        em.flush();return JourneyMapper.rating(ratings.saveAndFlush(rating));
    }
    @Transactional(readOnly=true) public RatingResponse rating(Long id) {
        own(id,false);return JourneyMapper.rating(ratings.findByVisitId(id).orElseThrow(()->new NotFoundException("Rating not found")));
    }
    public RouteResponse generate(Long id,RouteRequest request) {
        Visit visit=own(id,true);active(visit);
        List<Recommendation> candidates=recommendations.recommend(visit,request.criterion());
        if(candidates.isEmpty())throw new ConflictException("No unvisited available participation; a route cannot be empty");
        for(RecommendedRoute old:routes.findByVisitIdOrderByIdDesc(id))old.setStatus(VisitStatus.ZAVRSENA);
        RecommendedRoute route=new RecommendedRoute();route.setVisit(visit);route.setGeneratedAt(clock.instant());route.setCriterion(request.criterion());route.setStatus(VisitStatus.AKTIVNA);
        routes.saveAndFlush(route);List<RouteItem> saved=new ArrayList<>();int position=1;
        for(Recommendation candidate:candidates) {
            RouteItem item=new RouteItem();item.setId(new RouteItemId(route.getId(),position++));item.setRoute(route);
            item.setParticipation(em.getReference(ExhibitParticipation.class,candidate.participationId()));
            item.setExplanation(candidate.explanation());item.setVisited(false);saved.add(items.save(item));
        }
        return JourneyMapper.route(route,saved);
    }
    @Transactional(readOnly=true) public List<RouteResponse> routes(Long id) {
        own(id,false);return routes.findByVisitIdOrderByIdDesc(id).stream().map(r->JourneyMapper.route(r,items.findByRouteIdOrderByIdPosition(r.getId()))).toList();
    }
    @Transactional(readOnly=true) public List<Recommendation> next(Long id) {
        Visit visit=own(id,false);active(visit);return recommendations.recommend(visit,RouteCriterion.KOMBINACIJA).stream().limit(1).toList();
    }
    @Transactional(readOnly=true) public ParticipationContent content(Long id,Long participationId) {
        Visit visit=own(id,false);active(visit);
        ExhibitParticipation p=em.find(ExhibitParticipation.class,participationId);
        if(p==null)throw new NotFoundException("Participation not found");
        Instant now=clock.instant();
        if(!p.getExhibition().getId().equals(visit.getTicket().getTerm().getExhibition().getId())
                || (p.getInstalledAt()!=null && now.isBefore(p.getInstalledAt()))
                || (p.getRemovedAt()!=null && !now.isBefore(p.getRemovedAt())))
            throw new ConflictException("Participation is unavailable in this exhibition");
        var content=em.createQuery("select d from DigitalContent d where d.participation.id=:id order by d.version desc,d.id",DigitalContent.class)
                .setParameter("id",participationId).getResultList().stream()
                .map(d->new ContentResponse(d.getId(),d.getType().name(),d.getPath(),d.getLanguage(),d.getVersion())).toList();
        return new ParticipationContent(p.getId(),p.getExhibit().getName(),p.getContextDescription(),content);
    }
    private Visit own(Long id,boolean lock) {
        Visit visit=(lock?visits.lock(id):visits.findById(id)).orElseThrow(()->new NotFoundException("Visit not found"));
        if(visit.getTicket().getVisitor()==null || !visit.getTicket().getVisitor().getAccount().getId().equals(CurrentUser.id()))throw new AccessDeniedException("Visit belongs to another visitor");
        return visit;
    }
    private void active(Visit visit) {
        Instant now=clock.instant();var term=visit.getTicket().getTerm();
        if(visit.getStatus()!=VisitStatus.AKTIVNA || !now.isBefore(visit.getTicket().getValidUntil()) || !now.isBefore(term.getEnd())
                || term.getStatus()==TermStatus.OTKAZAN || term.getExhibition().getStatus()==ExhibitionStatus.OTKAZANA)
            throw new ConflictException("Active visit required");
    }
    private void close(Visit visit) {
        Instant now=clock.instant();Instant end=visit.getTicket().getTerm().getEnd();
        Instant exit=now.isBefore(end)?now:end;
        visit.setExitedAt(exit.isBefore(visit.getEnteredAt())?visit.getEnteredAt():exit);visit.setStatus(VisitStatus.ZAVRSENA);
        visit.getTicket().setStatus(TicketStatus.ISKORISCENA);
        routes.findByVisitIdOrderByIdDesc(visit.getId()).forEach(r->r.setStatus(VisitStatus.ZAVRSENA));
    }
    private ScanResponse scanDto(ExhibitScan s,List<Recommendation> next) {
        return new ScanResponse(s.getId(),s.getVisit().getId(),s.getParticipation().getId(),s.getScannedAt(),s.getDurationSeconds(),next);
    }
}
