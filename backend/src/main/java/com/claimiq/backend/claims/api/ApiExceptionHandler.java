package com.claimiq.backend.claims.api;

import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.exception.InvalidClaimTransitionException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ClaimNotFoundException.class)
    ResponseEntity<?> notFound(ClaimNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "CLAIM_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidClaimTransitionException.class)
    ResponseEntity<?> conflict(InvalidClaimTransitionException e) {
        return error(HttpStatus.CONFLICT, "INVALID_CLAIM_TRANSITION", e.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        IllegalArgumentException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<?> invalid(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed");
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(
                        Map.of(
                                "timestamp",
                                Instant.now().toString(),
                                "status",
                                status.value(),
                                "code",
                                code,
                                "message",
                                message));
    }
}
