package com.claimiq.backend.claims.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.claimiq.backend.claims.TestClaimData;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class ClaimContextTest {
    @Test
    void computesServiceTotalsExactlyAndRejectsMismatchedClaimAmount() {
        var context = TestClaimData.domain(new BigDecimal("125.50"));
        assertEquals(new BigDecimal("125.50"), context.total());
        assertThrows(
                IllegalArgumentException.class,
                () -> context.requireMatchingAmount(new BigDecimal("125.49")));
    }

    @Test
    void rejectsInvalidUnitsPriceDuplicateDiagnosesAndAuthorization() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ClaimServiceLine(
                                ClaimServiceLine.CodeSystem.LOCAL,
                                "DEMO-1",
                                "Service",
                                0,
                                BigDecimal.ONE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ClaimServiceLine(
                                ClaimServiceLine.CodeSystem.LOCAL,
                                "DEMO-1",
                                "Service",
                                1,
                                new BigDecimal("0.001")));
        var context = TestClaimData.domain(BigDecimal.ONE);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ClaimContext(
                                context.insurance(),
                                List.of("Z00.00", "Z00.00"),
                                context.serviceLines(),
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InsuranceDetails(
                                "Payer",
                                "Plan",
                                "Member",
                                InsuranceDetails.CoverageStatus.ACTIVE,
                                null,
                                InsuranceDetails.AuthorizationStatus.APPROVED,
                                null));
    }

    @Test
    void legacyDraftCanBeReadButCannotSubmitWithoutContext() {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-DEMO");
        claim.updateDetails("PAT", "PROV", BigDecimal.ONE, "USD", LocalDate.of(2026, 1, 1));
        assertNull(claim.getContext());
        assertThrows(IllegalArgumentException.class, claim::submit);
        assertEquals(ClaimStatus.DRAFT, claim.getStatus());
        assertNull(claim.getSubmittedAt());
    }

    @Test
    void invalidContextDoesNotPartiallyChangeAClaim() {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-DEMO");
        claim.updateDetails(
                "PAT",
                "PROV",
                BigDecimal.ONE,
                "USD",
                LocalDate.of(2026, 1, 1),
                TestClaimData.domain(BigDecimal.ONE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        claim.updateDetails(
                                "OTHER",
                                "PROV",
                                new BigDecimal("2.00"),
                                "USD",
                                LocalDate.of(2026, 1, 1),
                                TestClaimData.domain(BigDecimal.ONE)));
        assertEquals("PAT", claim.getPatientReference());
        assertEquals(BigDecimal.ONE, claim.getAmount());
        assertThrows(
                UnsupportedOperationException.class,
                () -> claim.getContext().diagnosisCodes().add("R69"));
    }
}
