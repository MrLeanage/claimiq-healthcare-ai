package com.claimiq.backend.claims.repository;

import com.claimiq.backend.claims.domain.Claim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    org.springframework.data.domain.Page<Claim> findAllByOwnerSubject(
            String ownerSubject, org.springframework.data.domain.Pageable pageable);
}
