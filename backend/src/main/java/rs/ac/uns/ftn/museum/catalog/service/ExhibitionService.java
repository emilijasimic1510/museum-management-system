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
public class ExhibitionService {
    private final ExhibitionRepository exhibitions;
    private final HallRepository halls;
    private final ExhibitionTermRepository terms;
    public ExhibitionService(ExhibitionRepository exhibitions, HallRepository halls, ExhibitionTermRepository terms) {
        this.exhibitions = exhibitions; this.halls = halls; this.terms = terms;
    }
    @Transactional(readOnly = true)
    public List<ExhibitionResponse> list() { return list(false); }
    @Transactional(readOnly = true)
    public List<ExhibitionResponse> list(boolean browse) {
        Instant now = Instant.now();
        return exhibitions.findAll(Sort.by("id")).stream()
            .filter(e -> staff() || Boolean.TRUE.equals(e.getOnlineVisible()))
            .filter(e -> !browse || browsable(e, now))
            .map(e -> browse ? CatalogMapper.exhibition(e, terms.findByExhibitionId(e.getId()).stream()
                    .filter(t -> t.getEnd().isAfter(now) && (t.getStatus()==TermStatus.OTVOREN || t.getStatus()==TermStatus.POPUNJEN)).toList()) : response(e)).toList();
    }
    public static boolean browsable(Exhibition e, Instant now) {
        return Boolean.TRUE.equals(e.getOnlineVisible()) && e.getEnd().isAfter(now)
                && (e.getStatus()==ExhibitionStatus.AKTIVNA || e.getStatus()==ExhibitionStatus.PLANIRANA);
    }
    @Transactional(readOnly = true)
    public ExhibitionResponse get(Long id) {
        Exhibition e=require(id);
        if (!staff() && !Boolean.TRUE.equals(e.getOnlineVisible())) throw new NotFoundException("Exhibition not found");
        return response(e);
    }
    private boolean staff() { return CurrentUser.hasRole("CURATOR") || CurrentUser.hasRole("ADMINISTRATOR"); }
    public ExhibitionResponse create(ExhibitionCreateRequest r) {
        ScheduleRules.interval(r.start(), r.end());
        if (r.terms() == null || r.terms().isEmpty()) throw new InvalidRequestException("At least one initial term is required");
        Hall hall = lockHall(r.hallId());
        checkOverlap(r.hallId(), null, r.start(), r.end(), r.status());
        for (InitialTermRequest t : r.terms()) ScheduleRules.term(t.start(), t.end(), t.capacity(), r.start(), r.end());
        Exhibition e = new Exhibition();
        CatalogMapper.apply(e, hall, r.name(), r.description(), r.start(), r.end(), r.status(), r.onlineVisible());
        exhibitions.save(e);
        for (InitialTermRequest t : r.terms()) {
            ExhibitionTerm term = new ExhibitionTerm();
            CatalogMapper.apply(term, e, t.start(), t.end(), t.capacity(), t.status()); terms.save(term);
        }
        return response(e);
    }
    public ExhibitionResponse update(Long id, ExhibitionUpdateRequest r) {
        ScheduleRules.interval(r.start(), r.end());
        Exhibition e = exhibitions.findLockedById(id).orElseThrow(() -> new NotFoundException("Exhibition not found: " + id));
        Hall hall = lockHall(r.hallId());
        checkOverlap(r.hallId(), id, r.start(), r.end(), r.status());
        for (ExhibitionTerm t : terms.findByExhibitionId(id)) ScheduleRules.term(t.getStart(), t.getEnd(), t.getCapacity(), r.start(), r.end());
        CatalogMapper.apply(e, hall, r.name(), r.description(), r.start(), r.end(), r.status(), r.onlineVisible());
        return response(e);
    }
    public void delete(Long id) {
        Exhibition e = exhibitions.findLockedById(id).orElseThrow(() -> new NotFoundException("Exhibition not found: " + id));
        terms.deleteAll(terms.findByExhibitionId(id)); terms.flush();
        exhibitions.delete(e); exhibitions.flush();
    }
    private Exhibition require(Long id) { return exhibitions.findById(id).orElseThrow(() -> new NotFoundException("Exhibition not found: " + id)); }
    private Hall lockHall(Long id) { return halls.findLockedById(id).orElseThrow(() -> new NotFoundException("Hall not found: " + id)); }
    private void checkOverlap(Long hallId, Long id, Instant start, Instant end, ExhibitionStatus status) {
        if (status != ExhibitionStatus.OTKAZANA && exhibitions.overlaps(hallId, id, start, end, ExhibitionStatus.OTKAZANA)) {
            throw new ConflictException("Exhibition overlaps another exhibition in this hall");
        }
    }
    private ExhibitionResponse response(Exhibition e) { return CatalogMapper.exhibition(e, terms.findByExhibitionId(e.getId())); }
}
