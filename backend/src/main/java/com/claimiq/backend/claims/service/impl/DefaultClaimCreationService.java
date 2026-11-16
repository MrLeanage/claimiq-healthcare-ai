package com.claimiq.backend.claims.service.impl;

import com.claimiq.backend.claims.domain.ClaimCreationRequest;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.exception.IdempotencyConflictException;
import com.claimiq.backend.claims.repository.ClaimCreationRequestRepository;
import com.claimiq.backend.claims.service.ClaimAccessPolicy;
import com.claimiq.backend.claims.service.ClaimCreationService;
import com.claimiq.backend.claims.service.ClaimRequestFingerprint;
import com.claimiq.backend.claims.service.ClaimService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/** Coordinates retry-safe creation without moving lifecycle rules out of ClaimService. */
@Service
public class DefaultClaimCreationService implements ClaimCreationService {
    private final ClaimService claims;
    private final ClaimAccessPolicy access;
    private final ClaimCreationRequestRepository requests;
    private final ClaimRequestFingerprint fingerprint;
    private final TransactionTemplate transaction;

    public DefaultClaimCreationService(
            ClaimService claims,
            ClaimAccessPolicy access,
            ClaimCreationRequestRepository requests,
            ClaimRequestFingerprint fingerprint,
            PlatformTransactionManager transactionManager) {
        this.claims = claims;
        this.access = access;
        this.requests = requests;
        this.fingerprint = fingerprint;
        transaction = new TransactionTemplate(transactionManager);
        // A uniqueness loser must finish rolling back before looking up the winner.
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ClaimResponse create(ClaimRequest input, UUID key) {
        // Scope keys by authenticated subject; another user's key cannot replay their claim.
        access.requireCreate();
        if (key == null) return claims.create(input);
        String subject = access.subject();
        String hash = fingerprint.of(input);
        try {
            return transaction.execute(
                    status -> {
                        var existing = requests.findByOwnerSubjectAndRequestKey(subject, key);
                        if (existing.isPresent()) return replay(existing.get(), hash);
                        var reservation =
                                requests.saveAndFlush(new ClaimCreationRequest(subject, key, hash));
                        // ClaimService joins this transaction; rollback removes reservation and
                        // claim together.
                        ClaimResponse result = claims.create(input);
                        reservation.complete(result.id());
                        return result;
                    });
        } catch (DataIntegrityViolationException collision) {
            // Another request may have committed this key while our insert was waiting.
            return transaction.execute(
                    status ->
                            requests.findByOwnerSubjectAndRequestKey(subject, key)
                                    .map(existing -> replay(existing, hash))
                                    .orElseThrow(() -> collision));
        }
    }

    private ClaimResponse replay(ClaimCreationRequest request, String hash) {
        if (!request.matches(hash)) throw new IdempotencyConflictException();
        if (request.getClaimId() == null) {
            throw new IllegalStateException("Incomplete committed creation request");
        }
        // Recheck access and return the current representation and ETag, never a stale snapshot.
        return claims.get(request.getClaimId());
    }
}
