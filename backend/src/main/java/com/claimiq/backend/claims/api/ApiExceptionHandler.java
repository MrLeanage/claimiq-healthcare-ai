package com.claimiq.backend.claims.api;

import com.claimiq.backend.claims.dto.ApiError;
import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.exception.InvalidClaimTransitionException;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ClaimNotFoundException.class)
    ResponseEntity<ApiError> notFound(ClaimNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "CLAIM_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidClaimTransitionException.class)
    ResponseEntity<ApiError> conflict(InvalidClaimTransitionException e) {
        return error(HttpStatus.CONFLICT, "INVALID_CLAIM_TRANSITION", e.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        IllegalArgumentException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<ApiError> invalid(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now().toString(), status.value(), code, message));
    }
}
