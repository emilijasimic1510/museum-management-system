package rs.ac.uns.ftn.museum.journey.service;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.exception.NotFoundException;
import rs.ac.uns.ftn.museum.people.repository.*;
import rs.ac.uns.ftn.museum.people.security.CurrentUser;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;

@Service
public class FutureExhibitionService {
    private final EntityManager em;
    private final VisitorRepository visitors;
    private final VisitorInterestRepository interests;
    private final GraphProjection graph;
    private final Clock clock;
    public FutureExhibitionService(EntityManager em,VisitorRepository visitors,VisitorInterestRepository interests,GraphProjection graph,Clock clock) {
        this.em=em;this.visitors=visitors;this.interests=interests;this.graph=graph;this.clock=clock;
    }
    @Transactional(readOnly=true)
    public List<ExhibitionRecommendation> recommend() {
        var visitor=visitors.findByAccountId(CurrentUser.id()).orElseThrow(()->new NotFoundException("Visitor profile not found"));
        List<Exhibition> exhibitions=em.createQuery("""
            select e from Exhibition e where e.onlineVisible=true and e.end>:now and e.status in
             (rs.ac.uns.ftn.museum.catalog.domain.ExhibitionStatus.PLANIRANA,rs.ac.uns.ftn.museum.catalog.domain.ExhibitionStatus.AKTIVNA)
             order by e.start,e.id
            """,Exhibition.class).setParameter("now",clock.instant()).getResultList();
        if(exhibitions.isEmpty())return List.of();
        List<String> topics=Boolean.TRUE.equals(visitor.getRecommendationsConsent())?
            interests.findByVisitorId(visitor.getId()).stream().map(i->i.getInterest().getName()).toList():List.of();
        Map<Long,Double> scores=new HashMap<>();
        if(Boolean.TRUE.equals(visitor.getRecommendationsConsent())) {
            List<ExhibitParticipation> participations=em.createQuery("select p from ExhibitParticipation p where p.exhibition in :exhibitions",ExhibitParticipation.class)
                    .setParameter("exhibitions",exhibitions).getResultList();
            Long previous=em.createQuery("""
                select s.participation.exhibit.id from ExhibitScan s where s.visit.ticket.visitor.id=:visitor
                order by s.scannedAt desc,s.id desc
                """,Long.class).setParameter("visitor",visitor.getId()).setMaxResults(1).getResultStream().findFirst().orElse(null);
            Map<Long,Double> ranked=graph.scores(previous,participations.stream().map(p->p.getExhibit().getId()).distinct().toList(),topics);
            participations.forEach(p->scores.merge(p.getExhibition().getId(),ranked.getOrDefault(p.getExhibit().getId(),0d),Double::sum));
            exhibitions.sort(Comparator.comparingDouble((Exhibition e)->scores.getOrDefault(e.getId(),0d)).reversed());
        }
        return exhibitions.stream().limit(10).map(e->new ExhibitionRecommendation(e.getId(),e.getName(),e.getStart(),e.getEnd(),
            scores.getOrDefault(e.getId(),0d)>0?"Prema interesovanjima i prethodnom obilasku":"Javno dostupna izložba; opšti pregled")).toList();
    }
}
