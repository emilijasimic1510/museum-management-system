package rs.ac.uns.ftn.museum.audio;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestControllerAdvice
public class AudioErrors {
    @ExceptionHandler(AudioException.class)
    ResponseEntity<ProblemDetail> audio(AudioException e) {
        var body=ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage());
        body.setProperty("code","LOCAL_AUDIO_ERROR");
        return ResponseEntity.status(503).body(body);
    }
}
