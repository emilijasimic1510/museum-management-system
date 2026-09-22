package rs.ac.uns.ftn.museum.catalog.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.catalog.dto.*;
import rs.ac.uns.ftn.museum.catalog.service.*;

@RestController
@RequestMapping("/api/halls")
public class HallController {
    private final HallService service;
    public HallController(HallService service) { this.service = service; }
    @GetMapping
    public List<HallResponse> list() { return service.list(); }
    @GetMapping("/{id}")
    public HallResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<HallResponse> create(@Valid @RequestBody HallRequest request) {
        HallResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/halls/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public HallResponse update(@PathVariable Long id, @Valid @RequestBody HallRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
