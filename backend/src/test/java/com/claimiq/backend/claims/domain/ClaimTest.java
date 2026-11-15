package com.claimiq.backend.claims.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.claimiq.backend.claims.exception.InvalidClaimTransitionException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

class ClaimTest {
    @Test
    void newClaimIsDraftWithoutSubmissionTime() {
        Claim claim = validClaim();
        assertEquals(ClaimStatus.DRAFT, claim.getStatus());
        assertNull(claim.getSubmittedAt());
    }

    @Test
    void submissionChangesStateAndRecordsTimeTogether() {
        Claim claim = validClaim();
        Instant before = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        claim.submit();
        Instant after = Instant.now();

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        assertNotNull(claim.getSubmittedAt());
        assertFalse(claim.getSubmittedAt().isBefore(before));
        assertFalse(claim.getSubmittedAt().isAfter(after));
    }

    @Test
    void onlyDocumentedStatusTransitionsAreAllowed() {
        for (ClaimStatus current : ClaimStatus.values()) {
            for (ClaimStatus target : ClaimStatus.values()) {
                Claim claim = claimAt(current);
                Instant submittedAt = claim.getSubmittedAt();
                boolean allowed =
                        (current == ClaimStatus.SUBMITTED && target == ClaimStatus.UNDER_REVIEW)
                                || (current == ClaimStatus.UNDER_REVIEW
                                        && (target == ClaimStatus.APPROVED
                                                || target == ClaimStatus.REJECTED));
                String description = current + " -> " + target;
                if (allowed) {
                    claim.transitionTo(target);
                    assertEquals(target, claim.getStatus(), description);
                } else {
                    var error =
                            assertThrows(
                                    InvalidClaimTransitionException.class,
                                    () -> claim.transitionTo(target),
                                    description);
                    assertEquals(
                            "Invalid transition from " + current + " to " + target,
                            error.getMessage());
                    assertEquals(current, claim.getStatus(), description);
                }
                assertEquals(submittedAt, claim.getSubmittedAt(), description);
            }
        }
    }

    @Test
    void nonDraftClaimsCannotBeSubmittedAgainOrEditedThroughSetters() {
        List<Consumer<Claim>> edits =
                List.of(
                        claim -> claim.setClaimNumber("CLM-UPDATED"),
                        claim -> claim.setPatientReference("PAT-UPDATED"),
                        claim -> claim.setProviderReference("PROV-UPDATED"),
                        claim -> claim.setAmount(new BigDecimal("250.00")),
                        claim -> claim.setCurrency("EUR"),
                        claim -> claim.setServiceDate(LocalDate.of(2026, 1, 2)));

        for (ClaimStatus status : ClaimStatus.values()) {
            if (status == ClaimStatus.DRAFT) continue;
            Claim claim = claimAt(status);
            Instant submittedAt = claim.getSubmittedAt();
            var error = assertThrows(InvalidClaimTransitionException.class, claim::submit);
            assertEquals("Only draft claims can be submitted", error.getMessage());
            for (Consumer<Claim> edit : edits) {
                var editError =
                        assertThrows(
                                InvalidClaimTransitionException.class, () -> edit.accept(claim));
                assertEquals("Only draft claims can be edited", editError.getMessage());
            }
            assertEquals(status, claim.getStatus());
            assertEquals(submittedAt, claim.getSubmittedAt());
            assertEquals("CLM-TEST", claim.getClaimNumber());
            assertEquals("PAT-DEMO", claim.getPatientReference());
            assertEquals("PROV-DEMO", claim.getProviderReference());
            assertEquals(new BigDecimal("125.00"), claim.getAmount());
            assertEquals("USD", claim.getCurrency());
            assertEquals(LocalDate.of(2026, 1, 1), claim.getServiceDate());
        }
    }

    @Test
    void draftDetailsRemainEditable() {
        Claim claim = validClaim();
        claim.setClaimNumber("CLM-DEMO");
        claim.setPatientReference("PAT-DEMO");
        claim.setProviderReference("PROV-DEMO");
        claim.setAmount(new BigDecimal("125.00"));
        claim.setCurrency("USD");
        claim.setServiceDate(LocalDate.of(2026, 1, 1));

        assertEquals("CLM-DEMO", claim.getClaimNumber());
        assertEquals("PAT-DEMO", claim.getPatientReference());
        assertEquals("PROV-DEMO", claim.getProviderReference());
        assertEquals(new BigDecimal("125.00"), claim.getAmount());
        assertEquals("USD", claim.getCurrency());
        assertEquals(LocalDate.of(2026, 1, 1), claim.getServiceDate());
        assertEquals(ClaimStatus.DRAFT, claim.getStatus());
    }

    @Test
    void nullTargetIsRejectedWithoutChangingTheClaim() {
        for (ClaimStatus current : ClaimStatus.values()) {
            Claim claim = claimAt(current);
            Instant submittedAt = claim.getSubmittedAt();
            assertThrows(InvalidClaimTransitionException.class, () -> claim.transitionTo(null));
            assertEquals(current, claim.getStatus());
            assertEquals(submittedAt, claim.getSubmittedAt());
        }
    }

    @Test
    void invalidDetailUpdateIsAtomic() {
        Claim claim = validClaim();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        claim.updateDetails(
                                "PAT-CHANGED",
                                "PROV-CHANGED",
                                BigDecimal.ZERO,
                                "USD",
                                LocalDate.of(2026, 1, 1)));
        assertEquals("PAT-DEMO", claim.getPatientReference());
        assertEquals("PROV-DEMO", claim.getProviderReference());
        assertEquals(new BigDecimal("125.00"), claim.getAmount());
    }

    private Claim validClaim() {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-TEST");
        claim.setPatientReference("PAT-DEMO");
        claim.setProviderReference("PROV-DEMO");
        claim.setAmount(new BigDecimal("125.00"));
        claim.setCurrency("USD");
        claim.setServiceDate(LocalDate.of(2026, 1, 1));
        claim.updateContext(com.claimiq.backend.claims.TestClaimData.domain(claim.getAmount()));
        return claim;
    }

    @Test
    void incompleteClaimCannotBeSubmitted() {
        Claim claim = new Claim();
        assertThrows(IllegalArgumentException.class, claim::submit);
        assertEquals(ClaimStatus.DRAFT, claim.getStatus());
        assertNull(claim.getSubmittedAt());
    }

    @Test
    void invalidValuesAreRejectedBeforeMutation() {
        Claim claim = validClaim();
        for (BigDecimal invalid :
                new BigDecimal[] {
                    null,
                    BigDecimal.ZERO,
                    new BigDecimal("-1"),
                    new BigDecimal("0.001"),
                    new BigDecimal("10000000000000000")
                }) {
            assertThrows(IllegalArgumentException.class, () -> claim.setAmount(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> claim.setPatientReference(" "));
        assertThrows(IllegalArgumentException.class, () -> claim.setCurrency("usd"));
        assertThrows(
                IllegalArgumentException.class,
                () -> claim.setServiceDate(LocalDate.now().plusDays(1)));
        assertEquals(new BigDecimal("125.00"), claim.getAmount());
        assertEquals("PAT-DEMO", claim.getPatientReference());
        assertEquals("USD", claim.getCurrency());
    }

    private Claim claimAt(ClaimStatus status) {
        Claim claim = validClaim();
        if (status == ClaimStatus.DRAFT) return claim;
        claim.submit();
        if (status == ClaimStatus.SUBMITTED) return claim;
        claim.transitionTo(ClaimStatus.UNDER_REVIEW);
        if (status != ClaimStatus.UNDER_REVIEW) claim.transitionTo(status);
        return claim;
    }
}
