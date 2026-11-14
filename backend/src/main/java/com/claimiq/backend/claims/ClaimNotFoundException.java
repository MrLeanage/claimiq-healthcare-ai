package com.claimiq.backend.claims;

import java.util.UUID;

public class ClaimNotFoundException extends RuntimeException {
    public ClaimNotFoundException(UUID id) {
        super("Claim not found: " + id);
    }
}
