package com.claimiq.backend.claims.service;

import com.claimiq.backend.claims.domain.Claim;

public interface ClaimAccessPolicy {
    String subject();

    boolean canReview();

    void requireReadPermission();

    void requireCreate();

    void requireRead(Claim claim);

    void requireWrite(Claim claim);

    void requireReview();
}
