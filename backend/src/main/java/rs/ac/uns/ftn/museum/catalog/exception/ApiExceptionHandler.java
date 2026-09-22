package rs.ac.uns.ftn.museum.catalog.exception;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.*;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NotFoundException ex) { return problem(404, ex.getMessage()); }
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> conflict(ConflictException ex) { return problem(409, ex.getMessage()); }
    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ProblemDetail> invalid(InvalidRequestException ex) { return problem(400, ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        body.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", Objects.requireNonNullElse(e.getDefaultMessage(), "Invalid value"))).toList());
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> malformed(Exception ex) { return problem(400, "Malformed request, identifier or enum value"); }
    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> integrity(Exception ex) { return problem(409, "Operation conflicts with existing data or a database constraint"); }
    @ExceptionHandler({TransactionSystemException.class, JpaSystemException.class})
    ResponseEntity<ProblemDetail> transaction(RuntimeException ex) {
        // Deferred PostgreSQL constraints can fail at transaction commit.
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getSQLState() != null
                    && (sql.getSQLState().startsWith("23") || sql.getSQLState().startsWith("40"))) {
                return problem(409, "Operation conflicts with existing data or a database constraint");
            }
        }
        return problem(500, "Database operation failed");
    }
    private ResponseEntity<ProblemDetail> problem(int status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail));
    }
}
