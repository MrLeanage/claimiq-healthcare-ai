package com.claimiq.backend.claims.dto;

import com.claimiq.backend.claims.domain.ClaimServiceLine.CodeSystem;
import com.claimiq.backend.claims.domain.InsuranceDetails.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ClaimContextDto(
        @NotNull @Valid Insurance insurance,
        @NotEmpty @Size(max = 20)
                List<@NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9.-]{0,19}") String> diagnosisCodes,
        @NotEmpty @Size(max = 50) List<@NotNull @Valid ServiceLine> serviceLines,
        @Size(max = 2000)
                @Schema(
                        description = "Optional synthetic clinical context; no patient names",
                        example = "Synthetic consultation for demonstration")
                String clinicalSummary) {
    public record Insurance(
            @NotBlank @Size(max = 150) @Schema(example = "Demo payer") String payerName,
            @NotBlank @Size(max = 100) @Schema(example = "PLAN-DEMO") String planReference,
            @NotBlank @Size(max = 100) @Schema(example = "MEM-DEMO") String memberReference,
            @NotNull CoverageStatus coverageStatus,
            @PastOrPresent LocalDate coverageVerifiedOn,
            @NotNull AuthorizationStatus authorizationStatus,
            @Size(max = 100) String authorizationReference) {}

    public record ServiceLine(
            @NotNull CodeSystem codeSystem,
            @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9.-]{0,19}") @Schema(example = "DEMO-001")
                    String procedureCode,
            @NotBlank @Size(max = 200) @Schema(example = "Synthetic consultation")
                    String description,
            @Min(1) @Max(999) @Schema(example = "1") int units,
            @NotNull
                    @DecimalMin("0.01")
                    @Digits(integer = 16, fraction = 2)
                    @Schema(example = "125.50")
                    BigDecimal unitPrice) {}
}
