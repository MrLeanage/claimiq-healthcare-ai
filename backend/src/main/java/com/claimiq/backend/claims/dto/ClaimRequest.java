package com.claimiq.backend.claims.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(
        @NotBlank @Size(max = 100) String patientReference,
        @NotBlank @Size(max = 100) String providerReference,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull @PastOrPresent LocalDate serviceDate) {}
