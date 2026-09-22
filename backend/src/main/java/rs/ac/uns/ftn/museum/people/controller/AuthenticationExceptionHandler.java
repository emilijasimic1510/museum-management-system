package rs.ac.uns.ftn.museum.people.controller;

import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class AuthenticationExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> authentication(AuthenticationException ex) {
        return ResponseEntity.status(401).body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email, password or authentication"));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied"));
    }
}
