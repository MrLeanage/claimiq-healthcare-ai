package com.claimiq.backend.claims.service.impl;

import com.claimiq.backend.claims.service.ClaimNumberGenerator;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidClaimNumberGenerator implements ClaimNumberGenerator {
    @Override
    public String generate() {
        return "CLM-"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 20)
                        .toUpperCase(Locale.ROOT);
    }
}
