package rs.ac.uns.ftn.museum.audio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import rs.ac.uns.ftn.museum.catalog.dto.ExhibitDtos.DigitalContentResponse;
@RestController
@RequestMapping("/api")
public class AudioController {
    private final AudioService service;
    public AudioController(AudioService service){this.service=service;}
    public record GenerateRequest(String language){}
    @PostMapping("/curator/participations/{id}/audio")
    @ResponseStatus(HttpStatus.CREATED)
    public DigitalContentResponse generate(@PathVariable Long id,@RequestBody GenerateRequest request){return service.generate(id,request.language());}
    @GetMapping("/visits/{visitId}/content/{participationId}/audio/{contentId}")
    public ResponseEntity<byte[]> download(@PathVariable Long visitId,@PathVariable Long participationId,@PathVariable Long contentId){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav"))
            .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .body(service.download(visitId,participationId,contentId));
    }
}
