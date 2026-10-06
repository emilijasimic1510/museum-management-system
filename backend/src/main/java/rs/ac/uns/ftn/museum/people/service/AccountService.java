package rs.ac.uns.ftn.museum.people.service;

import java.util.*;
import java.time.*;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.museum.notification.EmailService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import rs.ac.uns.ftn.museum.people.domain.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.repository.*;
import rs.ac.uns.ftn.museum.people.mapper.PeopleMapper;
import rs.ac.uns.ftn.museum.people.security.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
@Service
@Transactional
public class AccountService {
    private final EmailService email;
    private final AccountRepository accounts;
    private final AccountRoleRepository assignments;
    private final RoleRepository roles;
    private final VisitorRepository visitors;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final Clock clock;
    private final String dummyHash;
    public AccountService(AccountRepository accounts, AccountRoleRepository assignments, RoleRepository roles,
            VisitorRepository visitors, PasswordEncoder encoder, TokenService tokens, Clock clock, EmailService email) {
        this.email=email;this.accounts=accounts;this.assignments=assignments;this.roles=roles;this.visitors=visitors;
        this.encoder=encoder;this.tokens=tokens;this.clock=clock;this.dummyHash=encoder.encode(UUID.randomUUID().toString());
    }
    public AccountResponse register(RegisterRequest r) {
        String email=normalizeEmail(r.email());
        if (accounts.existsByEmail(email)) throw new ConflictException("Email already registered");
        if (r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new InvalidRequestException("Password exceeds BCrypt's 72-byte limit");
        Account a=new Account();a.setEmail(email);a.setPasswordHash(encoder.encode(r.password()));
        a.setActive(true);a.setRegisteredAt(clock.instant());accounts.save(a);
        Role role=roles.findByName(RoleName.VISITOR).orElseThrow(() -> new ConflictException("VISITOR role must be configured"));
        assign(a,role,null);
        Visitor v=new Visitor();v.setAccount(a);v.setFirstName(r.firstName());v.setLastName(r.lastName());
        v.setPhone(r.phone());v.setRecommendationsConsent(r.recommendationsConsent());visitors.save(v);
        this.email.sendRegistrationConfirmation(a);
        return response(a);
    }
    public TokenResponse login(LoginRequest r) {
        Account a=accounts.findByEmail(normalizeEmail(r.email())).orElse(null);
        boolean matches;
        try { matches=encoder.matches(r.password(),a==null?dummyHash:a.getPasswordHash()); }
        catch (IllegalArgumentException ex) { matches=false; }
        if (!matches || a==null || !Boolean.TRUE.equals(a.getActive())) throw new BadCredentialsException("Invalid email or password");
        a.setLastLogin(clock.instant());return tokens.issue(a.getId());
    }
    @Transactional(readOnly=true)
    public AccountResponse me() { return response(require(CurrentUser.id())); }
    @Transactional(readOnly=true)
    public List<AccountResponse> list() { return accounts.findAll(Sort.by("id")).stream().map(this::response).toList(); }
    @Transactional(readOnly=true)
    public AccountResponse get(Long id) { return response(require(id)); }
    public AccountResponse update(Long id, AccountUpdateRequest r) {
        Account a=lock(id);String email=normalizeEmail(r.email());
        if (!a.getEmail().equals(email) && accounts.existsByEmail(email)) throw new ConflictException("Email already registered");
        a.setEmail(email);a.setActive(r.active());return response(a);
    }
    public void delete(Long id) {
        Account a=lock(id);assignments.deleteAll(assignments.findByAccountId(id));assignments.flush();accounts.delete(a);accounts.flush();
    }
    public AccountResponse grant(Long id, Long roleId) {
        Account a=lock(id);Role role=roles.findById(roleId).orElseThrow(() -> new NotFoundException("Role not found"));
        if (assignments.existsById(new AccountRoleId(id,roleId))) throw new ConflictException("Role already assigned");
        assign(a,role,require(CurrentUser.id()));return response(a);
    }
    public void revoke(Long id,Long roleId) {
        lock(id);AccountRole ar=assignments.findById(new AccountRoleId(id,roleId)).orElseThrow(() -> new NotFoundException("Assignment not found"));
        if (assignments.countByAccountId(id)<=1) throw new ConflictException("Account must retain at least one role");
        assignments.delete(ar);assignments.flush();
    }
    private void assign(Account a, Role r, Account by) {
        AccountRole ar=new AccountRole();ar.setId(new AccountRoleId(a.getId(),r.getId()));ar.setAccount(a);ar.setRole(r);ar.setAssignedBy(by);ar.setAssignedAt(clock.instant());assignments.save(ar);
    }
    public static String normalizeEmail(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private Account require(Long id) { return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account not found")); }
    private Account lock(Long id) { return accounts.lockById(id).orElseThrow(() -> new NotFoundException("Account not found")); }
    private AccountResponse response(Account a) { return new AccountResponse(a.getId(),a.getEmail(),a.getActive(),a.getRegisteredAt(),a.getLastLogin(),assignments.roleNames(a.getId())); }
}
