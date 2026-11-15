package com.claimiq.backend.claims.api;

import com.claimiq.backend.claims.dto.ApiError;
import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.exception.ClaimVersionConflictException;
import com.claimiq.backend.claims.exception.InvalidClaimTransitionException;
import com.claimiq.backend.claims.exception.VersionPreconditionRequiredException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

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

    @ExceptionHandler({
        ClaimVersionConflictException.class,
        OptimisticLockingFailureException.class
    })
    ResponseEntity<ApiError> stale(Exception e) {
        return error(
                HttpStatus.PRECONDITION_FAILED,
                "STALE_CLAIM_VERSION",
                "Claim has changed; reload it before retrying");
    }

    @ExceptionHandler(VersionPreconditionRequiredException.class)
    ResponseEntity<ApiError> missingVersion(VersionPreconditionRequiredException e) {
        return error(HttpStatus.PRECONDITION_REQUIRED, "VERSION_REQUIRED", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidParameter(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(Exception e) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Insufficient claim permissions");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now().toString(), status.value(), code, message));
    }
}
