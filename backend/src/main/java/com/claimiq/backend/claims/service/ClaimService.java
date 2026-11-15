package com.claimiq.backend.claims.service;

import com.claimiq.backend.claims.domain.ClaimStatus;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Public use cases for managing claims. */
public interface ClaimService {
    ClaimResponse create(ClaimRequest request);

    ClaimResponse get(UUID id);

    Page<ClaimResponse> list(Pageable pageable);

    ClaimResponse update(UUID id, ClaimRequest request);

    ClaimResponse submit(UUID id);

    ClaimResponse transition(UUID id, ClaimStatus target);
}
