package com.claimiq.backend.claims.service;

/** Supplies a claim number without coupling claim creation to a numbering policy. */
public interface ClaimNumberGenerator {
    String generate();
}
