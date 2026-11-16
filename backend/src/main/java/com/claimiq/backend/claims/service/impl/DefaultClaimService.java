package com.claimiq.backend.claims.service.impl;

import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.domain.ClaimAuditEvent;
import com.claimiq.backend.claims.domain.ClaimStatus;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.dto.ClaimSummary;
import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.exception.ClaimVersionConflictException;
import com.claimiq.backend.claims.mapper.ClaimMapper;
import com.claimiq.backend.claims.repository.ClaimAuditRepository;
import com.claimiq.backend.claims.repository.ClaimRepository;
import com.claimiq.backend.claims.service.ClaimAccessPolicy;
import com.claimiq.backend.claims.service.ClaimNumberGenerator;
import com.claimiq.backend.claims.service.ClaimService;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DefaultClaimService implements ClaimService {
    private final ClaimRepository repository;
    private final ClaimMapper mapper;
    private final ClaimNumberGenerator numberGenerator;
    private final ClaimAccessPolicy access;
    private final ClaimAuditRepository audit;

    public DefaultClaimService(
            ClaimRepository repository,
            ClaimMapper mapper,
            ClaimNumberGenerator numberGenerator,
            ClaimAccessPolicy access,
            ClaimAuditRepository audit) {
        this.repository = repository;
        this.mapper = mapper;
        this.numberGenerator = numberGenerator;
        this.access = access;
        this.audit = audit;
    }

    @Override
    public ClaimResponse create(ClaimRequest request) {
        access.requireCreate();
        Claim claim = new Claim();
        claim.assignOwner(access.subject());
        mapper.apply(request, claim);
        claim.setClaimNumber(numberGenerator.generate());
        return persist(claim, "CREATE", null);
    }

    @Transactional(readOnly = true)
    @Override
    public ClaimResponse get(UUID id) {
        Claim claim = find(id);
        access.requireRead(claim);
        return mapper.toResponse(claim);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ClaimSummary> list(Pageable pageable) {
        access.requireReadPermission();
        if (pageable.isUnpaged() || pageable.getPageSize() > 100)
            throw new IllegalArgumentException("Use pages of at most 100 claims");
        return access.canReview()
                ? repository.findAllProjectedBy(pageable)
                : repository.findAllProjectedByOwnerSubject(access.subject(), pageable);
    }

    @Override
    public ClaimResponse update(UUID id, ClaimRequest request, long expectedVersion) {
        Claim claim = find(id);
        access.requireWrite(claim);
        requireVersion(claim, expectedVersion);
        mapper.apply(request, claim);
        return persist(claim, "UPDATE", ClaimStatus.DRAFT);
    }

    @Override
    public ClaimResponse submit(UUID id, long expectedVersion) {
        Claim claim = find(id);
        access.requireWrite(claim);
        requireVersion(claim, expectedVersion);
        claim.submit();
        return persist(claim, "SUBMIT", ClaimStatus.DRAFT);
    }

    @Override
    public ClaimResponse transition(UUID id, ClaimStatus target, long expectedVersion) {
        access.requireReview();
        Claim claim = find(id);
        requireVersion(claim, expectedVersion);
        ClaimStatus previous = claim.getStatus();
        claim.transitionTo(target);
        return persist(claim, "TRANSITION", previous);
    }

    private void requireVersion(Claim claim, long expectedVersion) {
        if (expectedVersion < 0 || claim.getVersion() != expectedVersion) {
            throw new ClaimVersionConflictException();
        }
    }

    private ClaimResponse persist(Claim claim, String action, ClaimStatus previous) {
        Claim saved = repository.saveAndFlush(claim);
        audit.save(new ClaimAuditEvent(saved, access.subject(), action, previous));
        return mapper.toResponse(saved);
    }

    private Claim find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }
}
