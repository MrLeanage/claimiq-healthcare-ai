package com.claimiq.backend.claims.exception;

public class VersionPreconditionRequiredException extends RuntimeException {
    public VersionPreconditionRequiredException() {
        super("Supply the claim ETag in the If-Match header");
    }
}
