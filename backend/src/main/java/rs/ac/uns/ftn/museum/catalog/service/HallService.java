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
public class HallService {
    private final HallRepository halls;
    private final MuseumRepository museums;
    public HallService(HallRepository halls, MuseumRepository museums) { this.halls = halls; this.museums = museums; }
    @Transactional(readOnly = true)
    public List<HallResponse> list() { return halls.findAll(Sort.by("id")).stream().map(CatalogMapper::hall).toList(); }
    @Transactional(readOnly = true)
    public HallResponse get(Long id) { return CatalogMapper.hall(require(id)); }
    public HallResponse create(HallRequest r) {
        Hall h = new Hall(); CatalogMapper.apply(h, r, museum(r.museumId())); return CatalogMapper.hall(halls.save(h));
    }
    public HallResponse update(Long id, HallRequest r) {
        Hall h = require(id); CatalogMapper.apply(h, r, museum(r.museumId())); return CatalogMapper.hall(h);
    }
    public void delete(Long id) { halls.delete(require(id)); halls.flush(); }
    private Hall require(Long id) { return halls.findById(id).orElseThrow(() -> new NotFoundException("Hall not found: " + id)); }
    private Museum museum(Long id) { return museums.findById(id).orElseThrow(() -> new NotFoundException("Museum not found: " + id)); }
}
