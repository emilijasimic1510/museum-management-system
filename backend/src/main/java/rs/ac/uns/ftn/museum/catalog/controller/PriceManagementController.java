package rs.ac.uns.ftn.museum.catalog.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.catalog.dto.PriceManagementDtos.*;
import rs.ac.uns.ftn.museum.catalog.service.PriceManagementService;

@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/prices/management")
public class PriceManagementController {
    private final PriceManagementService service;
    public PriceManagementController(PriceManagementService service){this.service=service;}
    @GetMapping public List<Response> list(@PathVariable Long exhibitionId){return service.list(exhibitionId);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable Long exhibitionId,@Valid @RequestBody Create request){return service.create(exhibitionId,request);}
    @PutMapping public Response update(@PathVariable Long exhibitionId,@RequestParam String category,@RequestParam Instant validFrom,
            @Valid @RequestBody Update request){return service.update(exhibitionId,category,validFrom,request);}
}
