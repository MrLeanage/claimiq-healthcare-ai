package com.claimiq.backend.claims;

import com.claimiq.backend.claims.domain.*;
import com.claimiq.backend.claims.dto.ClaimContextDto;

import java.math.BigDecimal;
import java.util.List;

public final class TestClaimData {
    private TestClaimData() {}

    public static ClaimContext domain(BigDecimal amount) {
        return new ClaimContext(
                new InsuranceDetails(
                        "Demo payer",
                        "PLAN-DEMO",
                        "MEM-DEMO",
                        InsuranceDetails.CoverageStatus.UNKNOWN,
                        null,
                        InsuranceDetails.AuthorizationStatus.NOT_REQUIRED,
                        null),
                List.of("Z00.00"),
                List.of(
                        new ClaimServiceLine(
                                ClaimServiceLine.CodeSystem.LOCAL,
                                "DEMO-001",
                                "Synthetic service",
                                1,
                                amount)),
                "Synthetic clinical context");
    }

    public static ClaimContextDto dto(BigDecimal amount) {
        return new ClaimContextDto(
                new ClaimContextDto.Insurance(
                        "Demo payer",
                        "PLAN-DEMO",
                        "MEM-DEMO",
                        InsuranceDetails.CoverageStatus.UNKNOWN,
                        null,
                        InsuranceDetails.AuthorizationStatus.NOT_REQUIRED,
                        null),
                List.of("Z00.00"),
                List.of(
                        new ClaimContextDto.ServiceLine(
                                ClaimServiceLine.CodeSystem.LOCAL,
                                "DEMO-001",
                                "Synthetic service",
                                1,
                                amount)),
                "Synthetic clinical context");
    }
}
