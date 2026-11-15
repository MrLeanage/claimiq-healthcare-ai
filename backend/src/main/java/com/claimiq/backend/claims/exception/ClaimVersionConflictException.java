package com.claimiq.backend.claims.exception;

public class ClaimVersionConflictException extends RuntimeException {
    public ClaimVersionConflictException() {
        super("Claim has changed; reload it before retrying");
    }
}
