package com.claimiq.backend.claims.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.domain.ClaimStatus;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.exception.InvalidClaimTransitionException;
import com.claimiq.backend.claims.mapper.ClaimMapper;
import com.claimiq.backend.claims.repository.ClaimRepository;
import com.claimiq.backend.claims.service.ClaimService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

class DefaultClaimServiceTest {
    com.claimiq.backend.claims.service.ClaimAccessPolicy access =
            mock(com.claimiq.backend.claims.service.ClaimAccessPolicy.class);
    com.claimiq.backend.claims.repository.ClaimAuditRepository audit =
            mock(com.claimiq.backend.claims.repository.ClaimAuditRepository.class);
    ClaimRepository repository = mock(ClaimRepository.class);
    ClaimService service =
            new DefaultClaimService(
                    repository, new ClaimMapper(), () -> "CLM-TEST-001", access, audit);
    ClaimRequest request =
            new ClaimRequest(
                    "PAT-100",
                    "PROV-100",
                    new BigDecimal("125.00"),
                    "USD",
                    LocalDate.of(2026, 10, 1));

    @BeforeEach
    void setup() {
        when(access.subject()).thenReturn("test-user");
        when(repository.saveAndFlush(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createsDraft() {
        ClaimResponse response = service.create(request);
        assertEquals(ClaimStatus.DRAFT, response.status());
        assertEquals("CLM-TEST-001", response.claimNumber());
        verify(repository).saveAndFlush(any());
    }

    @Test
    void submitsDraft() {
        UUID id = UUID.randomUUID();
        Claim claim = validClaim();
        when(repository.findById(id)).thenReturn(Optional.of(claim));
        ClaimResponse result = service.submit(id, 0);
        assertEquals(ClaimStatus.SUBMITTED, result.status());
        assertNotNull(result.submittedAt());
    }

    @Test
    void rejectsApprovalBeforeReview() {
        UUID id = UUID.randomUUID();
        Claim claim = validClaim();
        when(repository.findById(id)).thenReturn(Optional.of(claim));
        assertThrows(
                InvalidClaimTransitionException.class,
                () -> service.transition(id, ClaimStatus.APPROVED, 0));
    }

    @Test
    void missingClaim() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ClaimNotFoundException.class, () -> service.get(id));
    }

    @Test
    void preventsEditingSubmittedClaim() {
        UUID id = UUID.randomUUID();
        Claim claim = validClaim();
        claim.submit();
        when(repository.findById(id)).thenReturn(Optional.of(claim));
        assertThrows(InvalidClaimTransitionException.class, () -> service.update(id, request, 0));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void transitionsSubmittedClaimToReview() {
        UUID id = UUID.randomUUID();
        Claim claim = validClaim();
        claim.submit();
        when(repository.findById(id)).thenReturn(Optional.of(claim));

        ClaimResponse response = service.transition(id, ClaimStatus.UNDER_REVIEW, 0);

        assertEquals(ClaimStatus.UNDER_REVIEW, response.status());
        assertEquals(claim.getSubmittedAt(), response.submittedAt());
        verify(repository).saveAndFlush(claim);
    }

    @Test
    void repeatedSubmissionDoesNotSaveOrChangeSubmissionTime() {
        UUID id = UUID.randomUUID();
        Claim claim = validClaim();
        claim.submit();
        var submittedAt = claim.getSubmittedAt();
        when(repository.findById(id)).thenReturn(Optional.of(claim));

        assertThrows(InvalidClaimTransitionException.class, () -> service.submit(id, 0));

        assertEquals(submittedAt, claim.getSubmittedAt());
        verify(repository, never()).saveAndFlush(any());
    }

    private Claim validClaim() {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-TEST");
        claim.setPatientReference("PAT-DEMO");
        claim.setProviderReference("PROV-DEMO");
        claim.setAmount(new BigDecimal("125.00"));
        claim.setCurrency("USD");
        claim.setServiceDate(LocalDate.of(2026, 1, 1));
        return claim;
    }
}
