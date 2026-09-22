package rs.ac.uns.ftn.museum.people.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.service.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService service;
    public AuthController(AccountService service) { this.service = service; }
    @PostMapping("/register") public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.created(URI.create("/api/accounts/me")).body(service.register(request));
    }
    @PostMapping("/login") public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }
}
