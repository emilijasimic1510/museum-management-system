package rs.ac.uns.ftn.museum.people.controller;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.people.repository.GuideRepository;
import rs.ac.uns.ftn.museum.people.security.CurrentUser;
import rs.ac.uns.ftn.museum.catalog.exception.NotFoundException;

@RestController
@RequestMapping("/api/guides")
public class GuideController {
    private final GuideRepository guides;
    public GuideController(GuideRepository guides) { this.guides=guides; }
    public record OwnGuideResponse(Long id, String firstName, String lastName, String primaryLanguage) {}
    @GetMapping("/me") @Transactional(readOnly=true)
    public OwnGuideResponse me() {
        var g=guides.findByAccountId(CurrentUser.id()).orElseThrow(() -> new NotFoundException("Guide profile not found"));
        return new OwnGuideResponse(g.getId(),g.getFirstName(),g.getLastName(),g.getPrimaryLanguage());
    }
}
