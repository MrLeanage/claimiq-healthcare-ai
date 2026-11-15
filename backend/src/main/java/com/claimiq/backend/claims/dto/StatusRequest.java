package com.claimiq.backend.claims.dto;

import com.claimiq.backend.claims.domain.ClaimStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull ClaimStatus status) {}
