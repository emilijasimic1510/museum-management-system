package rs.ac.uns.ftn.museum.journey.service;

import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.journey.domain.*;
import rs.ac.uns.ftn.museum.journey.repository.ExhibitScanRepository;
import rs.ac.uns.ftn.museum.people.repository.VisitorInterestRepository;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;

@Service
public class RecommendationService {
    private final EntityManager em;
    private final ExhibitScanRepository scans;
    private final VisitorInterestRepository interests;
    private final GraphProjection graph;
    private final Clock clock;
    public RecommendationService(EntityManager em,ExhibitScanRepository scans,VisitorInterestRepository interests,GraphProjection graph,Clock clock) {
        this.em=em;this.scans=scans;this.interests=interests;this.graph=graph;this.clock=clock;
    }
    public List<Recommendation> recommend(Visit visit,RouteCriterion criterion) {
        var exhibition=visit.getTicket().getTerm().getExhibition();
        Instant now=clock.instant();
        if(exhibition.getStatus()!=ExhibitionStatus.AKTIVNA || now.isBefore(exhibition.getStart()) || !now.isBefore(exhibition.getEnd())) return List.of();
        List<ExhibitScan> history=scans.findByVisitIdOrderByScannedAtAscIdAsc(visit.getId());
        Set<Long> seen=new HashSet<>();
        history.forEach(s->seen.add(s.getParticipation().getId()));
        List<ExhibitParticipation> available=em.createQuery("""
            select p from ExhibitParticipation p where p.exhibition.id=:id
             and (p.installedAt is null or p.installedAt<=:now)
             and (p.removedAt is null or p.removedAt>:now)
             order by p.tourOrder nulls last,p.id
            """,ExhibitParticipation.class).setParameter("id",exhibition.getId()).setParameter("now",now)
            .getResultList().stream().filter(p->!seen.contains(p.getId())).toList();
        var visitor=visit.getTicket().getVisitor();
        boolean consent=visitor!=null && Boolean.TRUE.equals(visitor.getRecommendationsConsent());
        if(!consent || available.isEmpty())return available.stream().map(p->dto(p,"Redosled obilaska kustosa")).toList();
        List<String> topics=interests.findByVisitorId(visitor.getId()).stream().map(i->i.getInterest().getName()).toList();
        Long previous=history.isEmpty()?null:history.getLast().getParticipation().getExhibit().getId();
        Map<Long,Double> scores=graph.scores(previous,available.stream().map(p->p.getExhibit().getId()).distinct().toList(),topics,criterion);
        // Without usable graph data, the approved curator order is the fallback.
        if(scores.isEmpty() || scores.values().stream().noneMatch(s->s>0))
            return available.stream().map(p->dto(p,"Redosled kustosa; nema dovoljno podataka za personalizaciju")).toList();
        List<ExhibitParticipation> ordered=new ArrayList<>(available);
        Integer lastOrder=history.isEmpty()?null:history.getLast().getParticipation().getTourOrder();
        if(criterion==RouteCriterion.BLIZINA && lastOrder!=null)
            ordered.sort(Comparator.comparingInt(p->p.getTourOrder()==null?Integer.MAX_VALUE:Math.abs(p.getTourOrder()-lastOrder)));
        else ordered.sort(Comparator.comparingDouble((ExhibitParticipation p)->scores.getOrDefault(p.getExhibit().getId(),0d)).reversed());
        return ordered.stream().map(p->dto(p,"Povezanost sadržaja, interesovanja i prethodnih obilazaka")).toList();
    }
    private Recommendation dto(ExhibitParticipation p,String explanation) {
        return new Recommendation(p.getId(),p.getExhibit().getId(),p.getExhibit().getName(),explanation);
    }
}
