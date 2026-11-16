package com.claimiq.backend.claims.repository;

import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.dto.ClaimSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    Page<ClaimSummary> findAllProjectedBy(Pageable pageable);

    Page<ClaimSummary> findAllProjectedByOwnerSubject(String ownerSubject, Pageable pageable);
}
