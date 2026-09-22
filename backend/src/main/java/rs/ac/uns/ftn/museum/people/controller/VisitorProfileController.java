package rs.ac.uns.ftn.museum.people.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.service.*;

@RestController
@RequestMapping("/api/visitors/me")
public class VisitorProfileController {
    private final VisitorService service;
    public VisitorProfileController(VisitorService service) { this.service = service; }
    @GetMapping public VisitorResponse me() { return service.me(); }
    @PutMapping public VisitorResponse update(@Valid @RequestBody VisitorProfileRequest request) { return service.updateMe(request); }
    @GetMapping("/interests") public List<VisitorInterestResponse> interests() { return service.myInterests(); }
    @PutMapping("/interests/{id}") public VisitorInterestResponse select(@PathVariable Long id, @Valid @RequestBody InterestLevelRequest request) {
        return service.setInterest(id, request);
    }
    @DeleteMapping("/interests/{id}") public ResponseEntity<Void> remove(@PathVariable Long id) {
        service.removeInterest(id); return ResponseEntity.noContent().build();
    }
    @GetMapping("/registrations") public List<RegistrationResponse> registrations() { return service.myRegistrations(); }
}
