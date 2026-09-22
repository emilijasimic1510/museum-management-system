package rs.ac.uns.ftn.museum.people.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.dto.*;
import rs.ac.uns.ftn.museum.people.service.*;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController {
    private final VisitorService service;
    public VisitorController(VisitorService service) { this.service = service; }
    @GetMapping public List<VisitorResponse> list() { return service.list(); }
    @GetMapping("/{id}") public VisitorResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<VisitorResponse> create(@Valid @RequestBody VisitorRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/visitors/" + result.id())).body(result);
    }
    @PutMapping("/{id}") public VisitorResponse update(@PathVariable Long id, @Valid @RequestBody VisitorRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
