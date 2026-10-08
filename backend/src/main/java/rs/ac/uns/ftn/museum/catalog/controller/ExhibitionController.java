package rs.ac.uns.ftn.museum.catalog.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.catalog.dto.*;
import rs.ac.uns.ftn.museum.catalog.service.*;

@RestController
@RequestMapping("/api/exhibitions")
public class ExhibitionController {
    private final ExhibitionService service;
    public ExhibitionController(ExhibitionService service) { this.service = service; }
    @GetMapping
    public List<ExhibitionResponse> list(@RequestParam(defaultValue = "false") boolean browse) { return service.list(browse); }
    @GetMapping("/{id}")
    public ExhibitionResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<ExhibitionResponse> create(@Valid @RequestBody ExhibitionCreateRequest request) {
        ExhibitionResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/exhibitions/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public ExhibitionResponse update(@PathVariable Long id, @Valid @RequestBody ExhibitionUpdateRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
