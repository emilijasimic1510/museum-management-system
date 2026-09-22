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
public class MuseumService {
    private final MuseumRepository museums;
    public MuseumService(MuseumRepository museums) { this.museums = museums; }
    @Transactional(readOnly = true)
    public List<MuseumResponse> list() { return museums.findAll(Sort.by("id")).stream().map(CatalogMapper::museum).toList(); }
    @Transactional(readOnly = true)
    public MuseumResponse get(Long id) { return CatalogMapper.museum(require(id)); }
    public MuseumResponse create(MuseumRequest request) {
        Museum m = new Museum(); CatalogMapper.apply(m, request);
        return CatalogMapper.museum(museums.save(m));
    }
    public MuseumResponse update(Long id, MuseumRequest request) {
        Museum m = require(id); CatalogMapper.apply(m, request); return CatalogMapper.museum(m);
    }
    public void delete(Long id) { museums.delete(require(id)); museums.flush(); }
    private Museum require(Long id) { return museums.findById(id).orElseThrow(() -> new NotFoundException("Museum not found: " + id)); }
}
