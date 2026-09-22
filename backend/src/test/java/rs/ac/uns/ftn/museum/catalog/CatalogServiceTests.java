package rs.ac.uns.ftn.museum.catalog;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.dto.*;
import rs.ac.uns.ftn.museum.catalog.repository.*;
import rs.ac.uns.ftn.museum.catalog.service.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTests {
    private static final Instant START = Instant.parse("2030-01-01T10:00:00Z");
    private static final Instant END = Instant.parse("2030-01-01T12:00:00Z");
    @Test void hallRequiresExistingMuseum() {
        HallRepository halls = mock(HallRepository.class);
        MuseumRepository museums = mock(MuseumRepository.class);
        when(museums.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new HallService(halls, museums).create(new HallRequest(99L, "A", null, null, null)))
                .isInstanceOf(NotFoundException.class);
        verify(halls, never()).save(any());
    }
    @Test void exhibitionRequiresExistingHall() {
        HallRepository halls = mock(HallRepository.class);
        when(halls.findLockedById(99L)).thenReturn(Optional.empty());
        var service = new ExhibitionService(mock(ExhibitionRepository.class), halls, mock(ExhibitionTermRepository.class));
        var initial = new InitialTermRequest(START, END, 0, TermStatus.OTVOREN);
        assertThatThrownBy(() -> service.create(new ExhibitionCreateRequest(99L, "E", null, START, END,
                ExhibitionStatus.PLANIRANA, true, java.util.List.of(initial)))).isInstanceOf(NotFoundException.class);
    }
    @Test void exhibitionRequiresInitialTerm() {
        var service = new ExhibitionService(mock(ExhibitionRepository.class), mock(HallRepository.class), mock(ExhibitionTermRepository.class));
        assertThatThrownBy(() -> service.create(new ExhibitionCreateRequest(1L, "E", null, START, END,
                ExhibitionStatus.PLANIRANA, true, java.util.List.of()))).isInstanceOf(InvalidRequestException.class);
    }
    @Test void termRequiresExistingExhibition() {
        ExhibitionRepository exhibitions = mock(ExhibitionRepository.class);
        when(exhibitions.findLockedById(99L)).thenReturn(Optional.empty());
        var service = new TermService(mock(ExhibitionTermRepository.class), exhibitions);
        assertThatThrownBy(() -> service.create(new TermRequest(99L, START, END, 1, TermStatus.OTVOREN)))
                .isInstanceOf(NotFoundException.class);
    }
    @Test void rejectsReversedInterval() {
        assertThatThrownBy(() -> ScheduleRules.interval(END, START)).isInstanceOf(InvalidRequestException.class);
    }
    @Test void rejectsEqualEndpoints() {
        assertThatThrownBy(() -> ScheduleRules.interval(START, START)).isInstanceOf(InvalidRequestException.class);
    }
    @Test void rejectsNegativeCapacity() {
        assertThatThrownBy(() -> ScheduleRules.term(START, END, -1, START, END)).isInstanceOf(InvalidRequestException.class);
    }
    @Test void acceptsZeroCapacityAndExactBoundaries() {
        assertThatCode(() -> ScheduleRules.term(START, END, 0, START, END)).doesNotThrowAnyException();
    }
    @Test void rejectsTermOutsideExhibition() {
        assertThatThrownBy(() -> ScheduleRules.term(START.minusSeconds(1), END, 1, START, END)).isInstanceOf(ConflictException.class);
    }
    @Test void compositeIdsHaveValueEquality() {
        assertThat(new AuthorshipId(1L, 2L)).isEqualTo(new AuthorshipId(1L, 2L));
        assertThat(new AuthorshipId(1L, 2L)).isNotEqualTo(new AuthorshipId(2L, 1L));
        assertThat(new ExhibitionPriceId(1L, START, "redovna")).isEqualTo(new ExhibitionPriceId(1L, START, "redovna"));
        assertThat(new ExhibitionPriceId(1L, START, "redovna").hashCode()).isEqualTo(new ExhibitionPriceId(1L, START, "redovna").hashCode());
    }
}
