package rs.ac.uns.ftn.museum.people;

import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.people.domain.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.repository.*;
import rs.ac.uns.ftn.museum.people.security.TokenService;
import rs.ac.uns.ftn.museum.people.service.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PeopleServiceTests {
    @ParameterizedTest @EnumSource(value=EventStatus.class, names={"POPUNJEN","ZAVRSEN","OTKAZAN"})
    void closedEventsRejectRegistrationEvenWithFreePlaces(EventStatus status) {
        Event event = new Event(); event.setStatus(status); event.setCapacity(10);
        assertThatThrownBy(() -> EventService.ensureOpen(event,0)).isInstanceOf(ConflictException.class);
    }
    @Test void capacityBoundaryAllowsOnlyRemainingPlaces() {
        Event event = new Event(); event.setStatus(EventStatus.PLANIRAN); event.setCapacity(2);
        assertThatCode(() -> EventService.ensureOpen(event,1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> EventService.ensureOpen(event,2)).isInstanceOf(ConflictException.class);
    }
    @Test void employeeRequiresExistingAccount() {
        AccountRepository accounts = mock(AccountRepository.class);
        EmployeeService service = new EmployeeService(mock(EmployeeRepository.class),accounts,
                mock(rs.ac.uns.ftn.museum.catalog.repository.MuseumRepository.class),mock(org.springframework.jdbc.core.JdbcTemplate.class));
        when(accounts.findById(99L)).thenReturn(Optional.empty());
        EmployeeRequest request = new EmployeeRequest(99L,1L,"Test","Employee",null,java.time.LocalDate.now(),null,EmployeeType.GUIDE,null,null,null,null,null);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(NotFoundException.class);
    }
    @Test void duplicateEmailDoesNotEncodeOrSaveNewCredentials() {
        AccountRepository accounts = mock(AccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AccountService service = new AccountService(accounts,mock(AccountRoleRepository.class),mock(RoleRepository.class),
                mock(VisitorRepository.class),encoder,mock(TokenService.class),Clock.systemUTC(),mock(rs.ac.uns.ftn.museum.notification.EmailService.class));
        clearInvocations(encoder);
        when(accounts.existsByEmail("visitor@example.test")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("VISITOR@example.test",java.util.UUID.randomUUID().toString(),"Test","Visitor",null,false);
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(encoder);
        verify(accounts,never()).save(any());
    }
    @Test void unknownEmailUsesDummyHashAndDoesNotIssueToken() {
        AccountRepository accounts = mock(AccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TokenService tokens = mock(TokenService.class);
        when(encoder.encode(any())).thenReturn("dummy");
        when(accounts.findByEmail(any())).thenReturn(Optional.empty());
        AccountService service = new AccountService(accounts,mock(AccountRoleRepository.class),mock(RoleRepository.class),
                mock(VisitorRepository.class),encoder,tokens,Clock.systemUTC(),mock(rs.ac.uns.ftn.museum.notification.EmailService.class));
        String password = java.util.UUID.randomUUID().toString();
        assertThatThrownBy(() -> service.login(new LoginRequest("missing@example.test",password))).isInstanceOf(BadCredentialsException.class);
        verify(encoder).matches(password,"dummy");
        verifyNoInteractions(tokens);
    }
}
