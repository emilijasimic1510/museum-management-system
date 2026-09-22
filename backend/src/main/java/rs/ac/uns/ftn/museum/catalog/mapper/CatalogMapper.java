package rs.ac.uns.ftn.museum.catalog.mapper;

import java.time.Instant;
import java.util.*;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.dto.*;

public final class CatalogMapper {
    private CatalogMapper() {}
    public static MuseumResponse museum(Museum m) {
        return new MuseumResponse(m.getId(), m.getName(), m.getFoundationYear(), m.getAddress(), m.getDescription(), m.getActive());
    }
    public static void apply(Museum m, MuseumRequest r) {
        m.setName(r.name()); m.setFoundationYear(r.foundationYear()); m.setAddress(r.address());
        m.setDescription(r.description()); m.setActive(r.active());
    }
    public static HallResponse hall(Hall h) {
        return new HallResponse(h.getId(), h.getMuseum().getId(), h.getName(), h.getArea(), h.getFloor(), h.getCapacity());
    }
    public static void apply(Hall h, HallRequest r, Museum museum) {
        h.setMuseum(museum); h.setName(r.name()); h.setArea(r.area()); h.setFloor(r.floor()); h.setCapacity(r.capacity());
    }
    public static ExhibitionResponse exhibition(Exhibition e, List<ExhibitionTerm> terms) {
        return new ExhibitionResponse(e.getId(), e.getHall().getId(), e.getName(), e.getDescription(),
                e.getStart(), e.getEnd(), e.getStatus(), e.getOnlineVisible(),
                terms.stream().sorted(Comparator.comparing(ExhibitionTerm::getStart).thenComparing(ExhibitionTerm::getId)).map(CatalogMapper::term).toList());
    }
    public static void apply(Exhibition e, Hall hall, String name, String description, Instant start,
            Instant end, ExhibitionStatus status, Boolean onlineVisible) {
        e.setHall(hall); e.setName(name); e.setDescription(description); e.setStart(start);
        e.setEnd(end); e.setStatus(status); e.setOnlineVisible(onlineVisible);
    }
    public static TermResponse term(ExhibitionTerm t) {
        return new TermResponse(t.getId(), t.getExhibition().getId(), t.getStart(), t.getEnd(), t.getCapacity(), t.getStatus());
    }
    public static void apply(ExhibitionTerm t, Exhibition e, Instant start, Instant end, Integer capacity, TermStatus status) {
        t.setExhibition(e); t.setStart(start); t.setEnd(end); t.setCapacity(capacity); t.setStatus(status);
    }
}
