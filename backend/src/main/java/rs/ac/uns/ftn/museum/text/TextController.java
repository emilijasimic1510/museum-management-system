package rs.ac.uns.ftn.museum.text;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class TextController {
 private final TextService service; public TextController(TextService s){service=s;}
 public record SaveRequest(String language,String text){}
 @PostMapping("/curator/participations/{id}/texts") public TextService.TextResponse save(@PathVariable Long id,@RequestBody SaveRequest r){return service.save(id,r.language(),r.text());}
 @GetMapping("/curator/participations/{id}/texts/{language}") public TextService.TextResponse curator(@PathVariable Long id,@PathVariable String language){return service.curator(id,language);}
 @GetMapping("/visits/{visit}/content/{participation}/text/{content}") public TextService.TextResponse visitor(@PathVariable Long visit,@PathVariable Long participation,@PathVariable Long content){return service.visitor(visit,participation,content);}
}
