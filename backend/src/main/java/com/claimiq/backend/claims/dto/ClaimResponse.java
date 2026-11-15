package com.claimiq.backend.claims.dto;

import com.claimiq.backend.claims.domain.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimResponse(
        UUID id,
        String claimNumber,
        String patientReference,
        String providerReference,
        BigDecimal amount,
        String currency,
        LocalDate serviceDate,
        ClaimStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        long version,
        ClaimContextDto context) {
    public ClaimResponse(
            UUID id,
            String claimNumber,
            String patientReference,
            String providerReference,
            BigDecimal amount,
            String currency,
            LocalDate serviceDate,
            ClaimStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            long version) {
        this(
                id,
                claimNumber,
                patientReference,
                providerReference,
                amount,
                currency,
                serviceDate,
                status,
                createdAt,
                updatedAt,
                submittedAt,
                version,
                null);
    }
}
