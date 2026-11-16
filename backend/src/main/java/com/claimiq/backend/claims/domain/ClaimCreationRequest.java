package com.claimiq.backend.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A request reservation and its claim are committed in the same transaction. */
@Entity
@Table(
        name = "claim_creation_requests",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_claim_creation_request",
                        columnNames = {"owner_subject", "request_key"}))
public class ClaimCreationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Nationalized
    @Column(nullable = false, updatable = false, length = 255)
    private String ownerSubject;

    @Column(nullable = false, updatable = false)
    private UUID requestKey;

    @Column(nullable = false, updatable = false, length = 64)
    private String requestHash;

    private UUID claimId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ClaimCreationRequest() {}

    public ClaimCreationRequest(String ownerSubject, UUID requestKey, String requestHash) {
        this.ownerSubject = Objects.requireNonNull(ownerSubject);
        this.requestKey = Objects.requireNonNull(requestKey);
        this.requestHash = Objects.requireNonNull(requestHash);
        this.createdAt = Instant.now();
    }

    public boolean matches(String hash) {
        return requestHash.equals(hash);
    }

    public void complete(UUID createdClaimId) {
        if (claimId != null)
            throw new IllegalStateException("Creation request is already complete");
        claimId = Objects.requireNonNull(createdClaimId);
    }

    public UUID getClaimId() {
        return claimId;
    }
}
