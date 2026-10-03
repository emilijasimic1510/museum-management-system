package rs.ac.uns.ftn.museum.catalog.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.catalog.dto.ExhibitDtos.*;
import rs.ac.uns.ftn.museum.catalog.service.ExhibitCatalogService;

@RestController
@RequestMapping("/api")
public class ExhibitCatalogController {
    private final ExhibitCatalogService service;
    public ExhibitCatalogController(ExhibitCatalogService service) { this.service=service; }

    @GetMapping("/exhibits") public List<ExhibitResponse> exhibits() { return service.exhibits(); }
    @GetMapping("/exhibits/{id}") public ExhibitResponse exhibit(@PathVariable Long id) { return service.exhibit(id); }
    @GetMapping("/authors") public List<AuthorResponse> authors() { return service.authors(); }
    @GetMapping("/authors/{id}") public AuthorResponse author(@PathVariable Long id) { return service.author(id); }
    @GetMapping("/exhibits/{id}/authorships") public List<AuthorshipResponse> authorships(@PathVariable Long id) { return service.authorships(id); }
    @GetMapping("/exhibits/{id}/participations") public List<ParticipationResponse> exhibitParticipations(@PathVariable Long id) { return service.exhibitParticipations(id); }
    @GetMapping("/exhibitions/{id}/participations") public List<ParticipationResponse> exhibitionParticipations(@PathVariable Long id) { return service.exhibitionParticipations(id); }

    @GetMapping("/exhibits/{id}/management") public ManagedExhibitResponse managedExhibit(@PathVariable Long id) { return service.managedExhibit(id); }
    @PostMapping("/exhibits") public ResponseEntity<ManagedExhibitResponse> createExhibit(@Valid @RequestBody ExhibitRequest request) {
        var result=service.createExhibit(request);
        return ResponseEntity.created(URI.create("/api/exhibits/"+result.exhibit().id())).body(result);
    }
    @PutMapping("/exhibits/{id}") public ManagedExhibitResponse updateExhibit(@PathVariable Long id, @Valid @RequestBody ExhibitRequest request) { return service.updateExhibit(id,request); }
    @DeleteMapping("/exhibits/{id}") public ResponseEntity<Void> deleteExhibit(@PathVariable Long id) { service.deleteExhibit(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/authors") public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
        var result=service.createAuthor(request);
        return ResponseEntity.created(URI.create("/api/authors/"+result.id())).body(result);
    }
    @PutMapping("/authors/{id}") public AuthorResponse updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) { return service.updateAuthor(id,request); }
    @DeleteMapping("/authors/{id}") public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) { service.deleteAuthor(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/exhibit-participations") public List<ManagedParticipationResponse> managedParticipations(@RequestParam(required=false) Long exhibitionId) { return service.managedParticipations(exhibitionId); }
    @GetMapping("/exhibits/{id}/authorships/management") public List<ManagedAuthorshipResponse> managedAuthorships(@PathVariable Long id) { return service.managedAuthorships(id); }
    @PutMapping("/exhibits/{id}/authorships/{authorId}") public ManagedAuthorshipResponse saveAuthorship(@PathVariable Long id, @PathVariable Long authorId, @Valid @RequestBody AuthorshipRequest request) { return service.saveAuthorship(id,authorId,request); }
    @DeleteMapping("/exhibits/{id}/authorships/{authorId}") public ResponseEntity<Void> deleteAuthorship(@PathVariable Long id, @PathVariable Long authorId) { service.deleteAuthorship(id,authorId); return ResponseEntity.noContent().build(); }
    @GetMapping("/exhibit-participations/{id}") public ManagedParticipationResponse managedParticipation(@PathVariable Long id) { return service.managedParticipation(id); }
    @PostMapping("/exhibit-participations") public ResponseEntity<ManagedParticipationResponse> createParticipation(@Valid @RequestBody ParticipationRequest request) {
        var result=service.createParticipation(request);
        return ResponseEntity.created(URI.create("/api/exhibit-participations/"+result.id())).body(result);
    }
    @PutMapping("/exhibit-participations/{id}") public ManagedParticipationResponse updateParticipation(@PathVariable Long id, @Valid @RequestBody ParticipationRequest request) { return service.updateParticipation(id,request); }
    @DeleteMapping("/exhibit-participations/{id}") public ResponseEntity<Void> deleteParticipation(@PathVariable Long id) { service.deleteParticipation(id); return ResponseEntity.noContent().build(); }

    // Content references are not classified as public by the existing schema. These reads are staff-only.
    @GetMapping("/exhibit-participations/{id}/digital-content") public List<DigitalContentResponse> contents(@PathVariable Long id) { return service.contents(id); }
    @GetMapping("/digital-content/{id}") public DigitalContentResponse content(@PathVariable Long id) { return service.content(id); }
    @PostMapping("/digital-content") public ResponseEntity<DigitalContentResponse> createContent(@Valid @RequestBody DigitalContentRequest request) {
        var result=service.createContent(request);
        return ResponseEntity.created(URI.create("/api/digital-content/"+result.id())).body(result);
    }
    @PutMapping("/digital-content/{id}") public DigitalContentResponse updateContent(@PathVariable Long id, @Valid @RequestBody DigitalContentRequest request) { return service.updateContent(id,request); }
    @DeleteMapping("/digital-content/{id}") public ResponseEntity<Void> deleteContent(@PathVariable Long id) { service.deleteContent(id); return ResponseEntity.noContent().build(); }
}
