package com.claimiq.backend.claims.service;

import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;

import java.util.UUID;

/** Creation use case with an optional subject-scoped retry key. */
public interface ClaimCreationService {
    ClaimResponse create(ClaimRequest request, UUID key);
}
