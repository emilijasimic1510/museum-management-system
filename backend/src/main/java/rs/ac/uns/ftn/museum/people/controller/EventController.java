package rs.ac.uns.ftn.museum.people.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.service.*;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;
    public EventController(EventService service) { this.service = service; }
    @GetMapping public List<EventResponse> list(@RequestParam(defaultValue = "false") boolean browse) { return service.list(browse); }
    @GetMapping("/{id}") public EventResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/events/" + result.id())).body(result);
    }
    @PutMapping("/{id}") public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/registrations") public ResponseEntity<RegistrationResponse> register(@PathVariable Long id) {
        return ResponseEntity.created(URI.create("/api/visitors/me/registrations")).body(service.register(id));
    }
    @DeleteMapping("/{id}/registrations/me") public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancel(id); return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/registrations") public List<ManagedRegistrationResponse> registrations(@PathVariable Long id) { return service.registrations(id); }
    @PutMapping("/{id}/registrations/{visitorId}") public RegistrationResponse updateRegistration(
            @PathVariable Long id, @PathVariable Long visitorId, @Valid @RequestBody RegistrationUpdateRequest request) {
        return service.updateRegistration(id, visitorId, request);
    }
}
