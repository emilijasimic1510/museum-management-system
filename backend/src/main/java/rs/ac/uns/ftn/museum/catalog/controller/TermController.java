package rs.ac.uns.ftn.museum.catalog.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.catalog.dto.*;
import rs.ac.uns.ftn.museum.catalog.service.*;

@RestController
@RequestMapping("/api/exhibition-terms")
public class TermController {
    private final TermService service;
    public TermController(TermService service) { this.service = service; }
    @GetMapping
    public List<TermResponse> list() { return service.list(); }
    @GetMapping("/{id}")
    public TermResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<TermResponse> create(@Valid @RequestBody TermRequest request) {
        TermResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/exhibition-terms/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public TermResponse update(@PathVariable Long id, @Valid @RequestBody TermRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
