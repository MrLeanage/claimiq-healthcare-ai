package com.claimiq.backend.claims.domain;

import com.claimiq.backend.claims.exception.InvalidClaimTransitionException;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String claimNumber;

    @Column(nullable = false, length = 100)
    private String patientReference;

    @Column(nullable = false, length = 100)
    private String providerReference;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant submittedAt;

    @Column(length = 255, updatable = false)
    private String ownerSubject;

    @Version private long version;

    public void assignOwner(String subject) {
        requireDraft("Only draft claims can be edited");
        if (ownerSubject != null)
            throw new IllegalStateException("Claim ownership cannot be reassigned");
        requireReference(subject, 255, "ownerSubject");
        ownerSubject = subject;
    }

    public String getOwnerSubject() {
        return ownerSubject;
    }

    @PrePersist
    void created() {
        createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() {
        updatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public UUID getId() {
        return id;
    }

    public void updateDetails(
            String patientReference,
            String providerReference,
            BigDecimal amount,
            String currency,
            LocalDate serviceDate) {
        requireDraft("Only draft claims can be edited");
        requireReference(patientReference, 100, "patientReference");
        requireReference(providerReference, 100, "providerReference");
        requireAmount(amount);
        requireCurrency(currency);
        requireServiceDate(serviceDate);
        this.patientReference = patientReference;
        this.providerReference = providerReference;
        this.amount = amount;
        this.currency = currency;
        this.serviceDate = serviceDate;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String x) {
        requireDraft("Only draft claims can be edited");
        requireReference(x, 40, "claimNumber");
        claimNumber = x;
    }

    public String getPatientReference() {
        return patientReference;
    }

    public void setPatientReference(String x) {
        requireDraft("Only draft claims can be edited");
        requireReference(x, 100, "patientReference");
        patientReference = x;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String x) {
        requireDraft("Only draft claims can be edited");
        requireReference(x, 100, "providerReference");
        providerReference = x;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal x) {
        requireDraft("Only draft claims can be edited");
        requireAmount(x);
        amount = x;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String x) {
        requireDraft("Only draft claims can be edited");
        requireCurrency(x);
        currency = x;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate x) {
        requireDraft("Only draft claims can be edited");
        requireServiceDate(x);
        serviceDate = x;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void submit() {
        requireDraft("Only draft claims can be submitted");
        requireReference(claimNumber, 40, "claimNumber");
        requireReference(patientReference, 100, "patientReference");
        requireReference(providerReference, 100, "providerReference");
        requireAmount(amount);
        requireCurrency(currency);
        requireServiceDate(serviceDate);
        status = ClaimStatus.SUBMITTED;
        submittedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public void transitionTo(ClaimStatus target) {
        boolean valid =
                (status == ClaimStatus.SUBMITTED && target == ClaimStatus.UNDER_REVIEW)
                        || (status == ClaimStatus.UNDER_REVIEW
                                && (target == ClaimStatus.APPROVED
                                        || target == ClaimStatus.REJECTED));
        if (!valid) {
            throw new InvalidClaimTransitionException(
                    "Invalid transition from " + status + " to " + target);
        }
        status = target;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public long getVersion() {
        return version;
    }

    private static void requireReference(String value, int maximum, String field) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException("Invalid " + field);
        }
    }

    private static void requireAmount(BigDecimal value) {
        if (value == null
                || value.signum() <= 0
                || value.scale() > 2
                || value.precision() - value.scale() > 16) {
            throw new IllegalArgumentException(
                    "Amount must be positive with at most 16 integer and 2 fraction digits");
        }
    }

    private static void requireCurrency(String value) {
        if (value == null || !value.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("Invalid currency");
    }

    private static void requireServiceDate(LocalDate value) {
        if (value == null || value.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Invalid service date");
    }

    private void requireDraft(String message) {
        if (status != ClaimStatus.DRAFT) {
            throw new InvalidClaimTransitionException(message);
        }
    }
}
