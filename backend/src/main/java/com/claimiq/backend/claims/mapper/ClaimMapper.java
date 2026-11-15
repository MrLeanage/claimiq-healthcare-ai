package com.claimiq.backend.claims.mapper;

import com.claimiq.backend.claims.domain.*;
import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.dto.ClaimContextDto;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;

import org.springframework.stereotype.Component;

@Component
public class ClaimMapper {
    public void apply(ClaimRequest r, Claim c) {
        c.updateDetails(
                r.patientReference(),
                r.providerReference(),
                r.amount(),
                r.currency(),
                r.serviceDate(),
                toDomain(r.context()));
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
                c.getVersion(),
                toDto(c.getContext()));
    }

    private ClaimContext toDomain(ClaimContextDto dto) {
        if (dto == null) return null;
        if (dto.insurance() == null || dto.serviceLines() == null)
            throw new IllegalArgumentException("Incomplete claim context");
        var i = dto.insurance();
        return new ClaimContext(
                new InsuranceDetails(
                        i.payerName(),
                        i.planReference(),
                        i.memberReference(),
                        i.coverageStatus(),
                        i.coverageVerifiedOn(),
                        i.authorizationStatus(),
                        i.authorizationReference()),
                dto.diagnosisCodes(),
                dto.serviceLines().stream()
                        .map(
                                l ->
                                        new ClaimServiceLine(
                                                l.codeSystem(),
                                                l.procedureCode(),
                                                l.description(),
                                                l.units(),
                                                l.unitPrice()))
                        .toList(),
                dto.clinicalSummary());
    }

    private ClaimContextDto toDto(ClaimContext context) {
        if (context == null) return null;
        var i = context.insurance();
        return new ClaimContextDto(
                new ClaimContextDto.Insurance(
                        i.getPayerName(),
                        i.getPlanReference(),
                        i.getMemberReference(),
                        i.getCoverageStatus(),
                        i.getCoverageVerifiedOn(),
                        i.getAuthorizationStatus(),
                        i.getAuthorizationReference()),
                context.diagnosisCodes(),
                context.serviceLines().stream()
                        .map(
                                l ->
                                        new ClaimContextDto.ServiceLine(
                                                l.getCodeSystem(),
                                                l.getProcedureCode(),
                                                l.getDescription(),
                                                l.getUnits(),
                                                l.getUnitPrice()))
                        .toList(),
                context.clinicalSummary());
    }
}
