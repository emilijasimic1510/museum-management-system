package rs.ac.uns.ftn.museum.catalog.service;

import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.dto.*;
import rs.ac.uns.ftn.museum.catalog.repository.*;
import rs.ac.uns.ftn.museum.catalog.mapper.CatalogMapper;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.people.security.CurrentUser;

@Service
@Transactional
public class TermService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    private final ExhibitionTermRepository terms;
    private final ExhibitionRepository exhibitions;
    public TermService(ExhibitionTermRepository terms, ExhibitionRepository exhibitions) { this.terms = terms; this.exhibitions = exhibitions; }
    @Transactional(readOnly = true)
    public List<TermResponse> list() { return terms.findAll(Sort.by("id")).stream()
            .filter(t -> visible(t.getExhibition())).map(CatalogMapper::term).toList(); }
    @Transactional(readOnly = true)
    public TermResponse get(Long id) {
        ExhibitionTerm t=require(id);
        if (!visible(t.getExhibition())) throw new NotFoundException("Exhibition term not found");
        return CatalogMapper.term(t);
    }
    private boolean visible(Exhibition e) {
        return CurrentUser.hasRole("CURATOR") || CurrentUser.hasRole("ADMINISTRATOR") || Boolean.TRUE.equals(e.getOnlineVisible());
    }
    public TermResponse create(TermRequest r) {
        Exhibition e = lock(r.exhibitionId());
        ScheduleRules.term(r.start(), r.end(), r.capacity(), e.getStart(), e.getEnd());
        ExhibitionTerm t = new ExhibitionTerm(); CatalogMapper.apply(t, e, r.start(), r.end(), r.capacity(), r.status());
        return CatalogMapper.term(terms.save(t));
    }
    public TermResponse update(Long id, TermRequest r) {
        ExhibitionTerm t = require(id);
        Long previous = t.getExhibition().getId();
        // Deterministic parent lock order when moving a term between exhibitions.
        TreeSet<Long> ids = new TreeSet<>(List.of(previous, r.exhibitionId()));
        Map<Long, Exhibition> parents = new HashMap<>();
        for (Long parent : ids) parents.put(parent, lock(parent));
        // Serialize with checkout before inspecting tickets or changing a sold term.
        em.refresh(t, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        long tickets=em.createQuery("select count(t) from Ticket t where t.term.id=:id",Long.class).setParameter("id",id).getSingleResult();
        if(tickets>0 && (!previous.equals(r.exhibitionId()) || !t.getStart().equals(r.start()) || !t.getEnd().equals(r.end())))
            throw new ConflictException("A term with ticket history cannot change exhibition or dates");
        long occupied=em.createQuery("select count(t) from Ticket t where t.term.id=:id and t.status<>rs.ac.uns.ftn.museum.journey.domain.TicketStatus.OTKAZANA",Long.class).setParameter("id",id).getSingleResult();
        if(r.capacity()<occupied)throw new ConflictException("Capacity is below existing tickets");
        if (!previous.equals(r.exhibitionId())) ensureAnotherTerm(previous);
        Exhibition e = parents.get(r.exhibitionId());
        ScheduleRules.term(r.start(), r.end(), r.capacity(), e.getStart(), e.getEnd());
        CatalogMapper.apply(t, e, r.start(), r.end(), r.capacity(), r.status());
        if(tickets>0 && (r.status()==TermStatus.OTVOREN || r.status()==TermStatus.POPUNJEN))
            t.setStatus(occupied>=r.capacity()?TermStatus.POPUNJEN:TermStatus.OTVOREN);
        return CatalogMapper.term(t);
    }
    public void delete(Long id) {
        ExhibitionTerm t = require(id); Long exhibitionId = t.getExhibition().getId();
        lock(exhibitionId); ensureAnotherTerm(exhibitionId); terms.delete(t); terms.flush();
    }
    private void ensureAnotherTerm(Long id) {
        if (terms.countByExhibitionId(id) <= 1) throw new ConflictException("Cannot remove or move the last exhibition term");
    }
    private ExhibitionTerm require(Long id) { return terms.findById(id).orElseThrow(() -> new NotFoundException("Exhibition term not found: " + id)); }
    private Exhibition lock(Long id) { return exhibitions.findLockedById(id).orElseThrow(() -> new NotFoundException("Exhibition not found: " + id)); }
}
