package com.claimiq.backend.claims.repository;

import com.claimiq.backend.claims.domain.ClaimAuditEvent;

import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface ClaimAuditRepository extends Repository<ClaimAuditEvent, UUID> {
    ClaimAuditEvent save(ClaimAuditEvent event);

    java.util.List<ClaimAuditEvent> findAllByClaimIdOrderByOccurredAtAsc(UUID claimId);
}
