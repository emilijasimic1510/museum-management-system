package rs.ac.uns.ftn.museum.journey.controller;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.journey.service.FutureExhibitionService;
import static rs.ac.uns.ftn.museum.journey.dto.JourneyDtos.*;
@RestController
public class RecommendationController {
    private final FutureExhibitionService service;
    public RecommendationController(FutureExhibitionService service) { this.service=service; }
    @GetMapping("/api/recommendations/exhibitions")
    public List<ExhibitionRecommendation> exhibitions() { return service.recommend(); }
}
