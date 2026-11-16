package com.claimiq.backend.claims.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super(
                "This creation key was already used with different claim data. Open the claims list"
                        + " to check the saved draft.");
    }
}
