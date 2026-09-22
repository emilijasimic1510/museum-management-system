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

public final class ScheduleRules {
    private ScheduleRules() {}
    public static void interval(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidRequestException("End must be after start");
        }
    }
    public static void term(Instant start, Instant end, Integer capacity, Instant exhibitionStart, Instant exhibitionEnd) {
        interval(start, end);
        if (capacity == null || capacity < 0) throw new InvalidRequestException("Term capacity must not be negative");
        if (start.isBefore(exhibitionStart) || end.isAfter(exhibitionEnd)) {
            throw new ConflictException("Term must be within exhibition period");
        }
    }
}
