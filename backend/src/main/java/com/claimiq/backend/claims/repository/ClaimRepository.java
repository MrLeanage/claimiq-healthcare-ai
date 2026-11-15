package com.claimiq.backend.claims.repository;

import com.claimiq.backend.claims.domain.Claim;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {}
