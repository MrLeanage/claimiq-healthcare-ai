package com.claimiq.backend.claims.domain;

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
    @Version private long version;

    @PrePersist
    void created() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String x) {
        claimNumber = x;
    }

    public String getPatientReference() {
        return patientReference;
    }

    public void setPatientReference(String x) {
        patientReference = x;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String x) {
        providerReference = x;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal x) {
        amount = x;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String x) {
        currency = x;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate x) {
        serviceDate = x;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus x) {
        status = x;
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

    public void setSubmittedAt(Instant x) {
        submittedAt = x;
    }

    public long getVersion() {
        return version;
    }
}
