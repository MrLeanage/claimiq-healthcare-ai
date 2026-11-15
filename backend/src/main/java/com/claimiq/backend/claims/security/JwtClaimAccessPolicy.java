package com.claimiq.backend.claims.security;

import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.exception.ClaimNotFoundException;
import com.claimiq.backend.claims.service.ClaimAccessPolicy;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimAccessPolicy implements ClaimAccessPolicy {
    private Authentication authentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("JWT authentication is required");
        }
        String subject = authentication.getName();
        if (subject == null || subject.isBlank() || subject.length() > 255) {
            throw new AccessDeniedException("A valid subject is required");
        }
        return authentication;
    }

    @Override
    public String subject() {
        String subject = authentication().getName();
        if (subject == null || subject.isBlank() || subject.length() > 255) {
            throw new AccessDeniedException("A valid subject is required");
        }
        return subject;
    }

    private boolean has(String scope) {
        return authentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("SCOPE_" + scope));
    }

    @Override
    public boolean canReview() {
        return has("claims:review");
    }

    @Override
    public void requireReadPermission() {
        if (!has("claims:read") && !has("claims:write") && !canReview())
            throw new AccessDeniedException("Claim read permission is required");
    }

    @Override
    public void requireCreate() {
        if (!has("claims:write"))
            throw new AccessDeniedException("Claim write permission is required");
    }

    @Override
    public void requireRead(Claim claim) {
        requireReadPermission();
        if (!canReview() && !subject().equals(claim.getOwnerSubject()))
            throw new ClaimNotFoundException(claim.getId());
    }

    @Override
    public void requireWrite(Claim claim) {
        requireCreate();
        if (!subject().equals(claim.getOwnerSubject()))
            throw new ClaimNotFoundException(claim.getId());
    }

    @Override
    public void requireReview() {
        if (!canReview()) throw new AccessDeniedException("Claim review permission is required");
    }
}
