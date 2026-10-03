package rs.ac.uns.ftn.museum.manager;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {
    private final ManagerAnalyticsService service;
    public ManagerController(ManagerAnalyticsService service){this.service=service;}
    @GetMapping("/dashboard")
    public ManagerDtos.Dashboard dashboard(
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.dashboard(from,to);}
}
