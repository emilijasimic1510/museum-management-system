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

@Service
@Transactional
public class TermService {
    private final ExhibitionTermRepository terms;
    private final ExhibitionRepository exhibitions;
    public TermService(ExhibitionTermRepository terms, ExhibitionRepository exhibitions) { this.terms = terms; this.exhibitions = exhibitions; }
    @Transactional(readOnly = true)
    public List<TermResponse> list() { return terms.findAll(Sort.by("id")).stream().map(CatalogMapper::term).toList(); }
    @Transactional(readOnly = true)
    public TermResponse get(Long id) { return CatalogMapper.term(require(id)); }
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
        if (!previous.equals(r.exhibitionId())) ensureAnotherTerm(previous);
        Exhibition e = parents.get(r.exhibitionId());
        ScheduleRules.term(r.start(), r.end(), r.capacity(), e.getStart(), e.getEnd());
        CatalogMapper.apply(t, e, r.start(), r.end(), r.capacity(), r.status());
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
