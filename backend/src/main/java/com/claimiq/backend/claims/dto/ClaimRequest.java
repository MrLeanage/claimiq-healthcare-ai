package com.claimiq.backend.claims.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(
        @NotBlank @Size(max = 100) @Schema(example = "PAT-DEMO") String patientReference,
        @NotBlank @Size(max = 100) @Schema(example = "PROV-DEMO") String providerReference,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) @Schema(example = "125.50")
                BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") @Schema(example = "USD") String currency,
        @NotNull @PastOrPresent @Schema(example = "2026-01-01") LocalDate serviceDate,
        @Valid @Schema(description = "Optional for a draft; required before submission")
                ClaimContextDto context) {
    public ClaimRequest(
            String patientReference,
            String providerReference,
            BigDecimal amount,
            String currency,
            LocalDate serviceDate) {
        this(patientReference, providerReference, amount, currency, serviceDate, null);
    }
}
