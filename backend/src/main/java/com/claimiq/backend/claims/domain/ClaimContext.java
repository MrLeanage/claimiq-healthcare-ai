package com.claimiq.backend.claims.domain;

import java.math.BigDecimal;
import java.util.List;

/** Complete user-reported context; it does not certify clinical codes or eligibility. */
public record ClaimContext(
        InsuranceDetails insurance,
        List<String> diagnosisCodes,
        List<ClaimServiceLine> serviceLines,
        String clinicalSummary) {
    public ClaimContext {
        if (insurance == null
                || diagnosisCodes == null
                || diagnosisCodes.isEmpty()
                || diagnosisCodes.size() > 20
                || serviceLines == null
                || serviceLines.isEmpty()
                || serviceLines.size() > 50)
            throw new IllegalArgumentException(
                    "Insurance, diagnoses and billed services are required");
        if (diagnosisCodes.stream()
                        .anyMatch(c -> c == null || !c.matches("[A-Z0-9][A-Z0-9.-]{0,19}"))
                || diagnosisCodes.stream().distinct().count() != diagnosisCodes.size())
            throw new IllegalArgumentException("Diagnosis codes must be distinct code identifiers");
        if (serviceLines.stream().anyMatch(java.util.Objects::isNull))
            throw new IllegalArgumentException("Invalid service line");
        if (clinicalSummary != null && clinicalSummary.length() > 2000)
            throw new IllegalArgumentException("Clinical summary too long");
        diagnosisCodes = List.copyOf(diagnosisCodes);
        serviceLines = List.copyOf(serviceLines);
    }

    public BigDecimal total() {
        return serviceLines.stream()
                .map(ClaimServiceLine::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void requireMatchingAmount(BigDecimal amount) {
        if (amount == null || total().compareTo(amount) != 0)
            throw new IllegalArgumentException("Claim amount must equal the billed service total");
    }
}
