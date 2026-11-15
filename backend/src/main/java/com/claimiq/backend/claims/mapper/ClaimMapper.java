package com.claimiq.backend.claims.mapper;

import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import org.springframework.stereotype.Component;

@Component
public class ClaimMapper {
    public void apply(ClaimRequest r, Claim c) {
        c.setPatientReference(r.patientReference());
        c.setProviderReference(r.providerReference());
        c.setAmount(r.amount());
        c.setCurrency(r.currency());
        c.setServiceDate(r.serviceDate());
    }

    public ClaimResponse toResponse(Claim c) {
        return new ClaimResponse(
                c.getId(),
                c.getClaimNumber(),
                c.getPatientReference(),
                c.getProviderReference(),
                c.getAmount(),
                c.getCurrency(),
                c.getServiceDate(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getSubmittedAt(),
                c.getVersion());
    }
}
