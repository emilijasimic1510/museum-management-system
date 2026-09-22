package rs.ac.uns.ftn.museum.people.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.service.*;

@RestController
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }
    @GetMapping("/api/accounts/me") public AccountResponse me() { return service.me(); }
    @GetMapping("/api/admin/accounts") public List<AccountResponse> list() { return service.list(); }
    @GetMapping("/api/admin/accounts/{id}") public AccountResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping("/api/admin/accounts") public ResponseEntity<AccountResponse> create(@Valid @RequestBody RegisterRequest request) {
        var result = service.register(request);
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + result.id())).body(result);
    }
    @PutMapping("/api/admin/accounts/{id}") public AccountResponse update(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/api/admin/accounts/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
    @PostMapping("/api/admin/accounts/{id}/roles/{roleId}") public AccountResponse grant(@PathVariable Long id, @PathVariable Long roleId) {
        return service.grant(id, roleId);
    }
    @DeleteMapping("/api/admin/accounts/{id}/roles/{roleId}") public ResponseEntity<Void> revoke(@PathVariable Long id, @PathVariable Long roleId) {
        service.revoke(id, roleId); return ResponseEntity.noContent().build();
    }
}
