package rs.ac.uns.ftn.museum.catalog.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.catalog.dto.*;
import rs.ac.uns.ftn.museum.catalog.service.*;

@RestController
@RequestMapping("/api/museums")
public class MuseumController {
    private final MuseumService service;
    public MuseumController(MuseumService service) { this.service = service; }
    @GetMapping
    public List<MuseumResponse> list() { return service.list(); }
    @GetMapping("/{id}")
    public MuseumResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<MuseumResponse> create(@Valid @RequestBody MuseumRequest request) {
        MuseumResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/museums/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public MuseumResponse update(@PathVariable Long id, @Valid @RequestBody MuseumRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
