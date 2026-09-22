package rs.ac.uns.ftn.museum.people.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.service.*;

@RestController
@RequestMapping("/api/interests")
public class InterestController {
    private final InterestService service;
    public InterestController(InterestService service) { this.service = service; }
    @GetMapping public List<InterestResponse> list() { return service.list(); }
    @GetMapping("/{id}") public InterestResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<InterestResponse> create(@Valid @RequestBody InterestRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/interests/" + result.id())).body(result);
    }
    @PutMapping("/{id}") public InterestResponse update(@PathVariable Long id, @Valid @RequestBody InterestRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
