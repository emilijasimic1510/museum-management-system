package rs.ac.uns.ftn.museum.journey.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.journey.service.VisitService;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final VisitService visits;
    public VisitController(VisitService visits) { this.visits=visits; }
    @GetMapping("/me") public List<VisitResponse> mine() { return visits.mine(); }
    @GetMapping("/{id}") public VisitResponse get(@PathVariable Long id) { return visits.get(id); }
    @PostMapping("/{id}/finish") public VisitResponse finish(@PathVariable Long id) { return visits.finish(id); }
    @PostMapping("/{id}/scans") public ResponseEntity<ScanResponse> scan(@PathVariable Long id,@Valid @RequestBody ScanRequest request) {
        return ResponseEntity.created(URI.create("/api/visits/"+id+"/scans")).body(visits.scan(id,request));
    }
    @GetMapping("/{id}/scans") public List<ScanResponse> scans(@PathVariable Long id) { return visits.scans(id); }
    @PostMapping("/{id}/rating") public ResponseEntity<RatingResponse> rate(@PathVariable Long id,@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.created(URI.create("/api/visits/"+id+"/rating")).body(visits.rate(id,request));
    }
    @GetMapping("/{id}/rating") public RatingResponse rating(@PathVariable Long id) { return visits.rating(id); }
    @PostMapping("/{id}/routes") public ResponseEntity<RouteResponse> generate(@PathVariable Long id,@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.created(URI.create("/api/visits/"+id+"/routes")).body(visits.generate(id,request));
    }
    @GetMapping("/{id}/routes") public List<RouteResponse> routes(@PathVariable Long id) { return visits.routes(id); }
    @GetMapping("/{id}/recommendations") public List<Recommendation> next(@PathVariable Long id) { return visits.next(id); }
    @GetMapping("/{id}/content/{participationId}") public ParticipationContent content(@PathVariable Long id,@PathVariable Long participationId) {
        return visits.content(id,participationId);
    }
}
