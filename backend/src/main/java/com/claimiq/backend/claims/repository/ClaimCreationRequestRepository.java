package com.claimiq.backend.claims.repository;

import com.claimiq.backend.claims.domain.ClaimCreationRequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClaimCreationRequestRepository extends JpaRepository<ClaimCreationRequest, UUID> {
    Optional<ClaimCreationRequest> findByOwnerSubjectAndRequestKey(
            String ownerSubject, UUID requestKey);
}
