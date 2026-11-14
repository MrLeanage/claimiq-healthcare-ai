package com.claimiq.backend.claims;

public class InvalidClaimTransitionException extends RuntimeException {
    public InvalidClaimTransitionException(String message) {
        super(message);
    }
}
