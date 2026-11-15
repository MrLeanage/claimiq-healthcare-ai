package com.claimiq.backend.claims.service.impl;

import com.claimiq.backend.claims.service.ClaimService;

import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.domain.ClaimStatus;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.mapper.ClaimMapper;
import com.claimiq.backend.claims.repository.ClaimRepository;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultClaimService implements ClaimService {
    private final ClaimRepository repository;
    private final ClaimMapper mapper;

    public DefaultClaimService(ClaimRepository repository, ClaimMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ClaimResponse create(ClaimRequest request) {
        Claim claim = new Claim();
        mapper.apply(request, claim);
        claim.setClaimNumber(
                "CLM-"
                        + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 20)
                                .toUpperCase());
        return mapper.toResponse(repository.save(claim));
    }

    @Transactional(readOnly = true)
    @Override
    public ClaimResponse get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ClaimResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    public ClaimResponse update(UUID id, ClaimRequest request) {
        Claim claim = find(id);
        mapper.apply(request, claim);
        return mapper.toResponse(repository.save(claim));
    }

    @Override
    public ClaimResponse submit(UUID id) {
        Claim claim = find(id);
        claim.submit();
        return mapper.toResponse(repository.save(claim));
    }

    @Override
    public ClaimResponse transition(UUID id, ClaimStatus target) {
        Claim claim = find(id);
        claim.transitionTo(target);
        return mapper.toResponse(repository.save(claim));
    }

    private Claim find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }
}
