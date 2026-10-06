package rs.ac.uns.ftn.museum.media;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ImageController {
    private final ImageService images;
    public ImageController(ImageService images){this.images=images;}
    private ResponseEntity<byte[]> response(byte[] bytes){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(bytes);}
    @GetMapping("/media/{kind:exhibits|exhibitions}/{id}") public ResponseEntity<byte[]> publicImage(@PathVariable String kind,@PathVariable Long id){return response(images.publicImage(kind,id));}
    @GetMapping("/curator/media/{kind:exhibits|exhibitions}/{id}") public ResponseEntity<byte[]> preview(@PathVariable String kind,@PathVariable Long id){return response(images.curatorPublicImage(kind,id));}
    @PostMapping(value="/curator/media/{kind:exhibits|exhibitions}/{id}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED) public ImageService.ImageResult upload(@PathVariable String kind,@PathVariable Long id,@RequestParam MultipartFile file){return images.uploadPublic(kind,id,file);}
    @PostMapping(value="/curator/participations/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED) public ImageService.ImageResult contextUpload(@PathVariable Long id,@RequestParam MultipartFile file){return images.uploadContext(id,file);}
    @GetMapping("/curator/participations/{id}/images/{content}") public ResponseEntity<byte[]> contextPreview(@PathVariable Long id,@PathVariable Long content){return response(images.curatorContext(id,content));}
    @GetMapping("/visits/{visit}/content/{participation}/images/{content}") public ResponseEntity<byte[]> context(@PathVariable Long visit,@PathVariable Long participation,@PathVariable Long content){return response(images.context(visit,participation,content));}
}
