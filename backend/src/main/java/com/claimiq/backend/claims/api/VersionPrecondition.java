package com.claimiq.backend.claims.api;

import com.claimiq.backend.claims.exception.VersionPreconditionRequiredException;

final class VersionPrecondition {
    private VersionPrecondition() {}

    // Accept one quoted version, not a wildcard/list/weak tag, for optimistic concurrency.
    static long parse(String header) {
        if (header == null || header.isBlank()) throw new VersionPreconditionRequiredException();
        if (!header.matches("\"[0-9]+\""))
            throw new IllegalArgumentException("A single strong claim ETag is required");
        try {
            return Long.parseLong(header.substring(1, header.length() - 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid claim ETag", e);
        }
    }
}
