package com.claimiq.backend.claims.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_audit_events")
public class ClaimAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID claimId;

    @Column(nullable = false, updatable = false, length = 255)
    private String actorSubject;

    @Column(nullable = false, updatable = false, length = 32)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, updatable = false)
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private ClaimStatus toStatus;

    @Column(nullable = false, updatable = false)
    private long claimVersion;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    protected ClaimAuditEvent() {}

    public ClaimAuditEvent(Claim claim, String actor, String action, ClaimStatus previousStatus) {
        this.claimId = claim.getId();
        this.actorSubject = actor;
        this.action = action;
        this.fromStatus = previousStatus;
        this.toStatus = claim.getStatus();
        this.claimVersion = claim.getVersion();
        this.occurredAt = Instant.now();
    }

    public UUID getClaimId() {
        return claimId;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getAction() {
        return action;
    }

    public ClaimStatus getFromStatus() {
        return fromStatus;
    }

    public ClaimStatus getToStatus() {
        return toStatus;
    }

    public long getClaimVersion() {
        return claimVersion;
    }
}
