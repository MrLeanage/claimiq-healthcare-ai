package com.claimiq.backend.claims.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.claimiq.backend.claims.service.ClaimNumberGenerator;
import org.junit.jupiter.api.Test;

class UuidClaimNumberGeneratorTest {
    @Test
    void preservesTheExistingClaimNumberFormat() {
        ClaimNumberGenerator generator = new UuidClaimNumberGenerator();
        assertTrue(generator.generate().matches("CLM-[0-9A-F]{20}"));
    }
}
