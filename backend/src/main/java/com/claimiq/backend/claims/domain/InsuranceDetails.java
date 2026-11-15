package com.claimiq.backend.claims.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

@Embeddable
public class InsuranceDetails {
    public enum CoverageStatus {
        UNKNOWN,
        ACTIVE,
        INACTIVE
    }

    public enum AuthorizationStatus {
        UNKNOWN,
        NOT_REQUIRED,
        PENDING,
        APPROVED,
        DENIED
    }

    @Nationalized
    @Column(name = "payer_name", length = 150)
    private String payerName;

    @Nationalized
    @Column(name = "plan_reference", length = 100)
    private String planReference;

    @Nationalized
    @Column(name = "member_reference", length = 100)
    private String memberReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", length = 20)
    private CoverageStatus coverageStatus;

    @Column(name = "coverage_verified_on")
    private LocalDate coverageVerifiedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_status", length = 20)
    private AuthorizationStatus authorizationStatus;

    @Nationalized
    @Column(name = "authorization_reference", length = 100)
    private String authorizationReference;

    protected InsuranceDetails() {}

    public InsuranceDetails(
            String payer,
            String plan,
            String member,
            CoverageStatus coverage,
            LocalDate verifiedOn,
            AuthorizationStatus authorization,
            String reference) {
        requireText(payer, 150);
        requireText(plan, 100);
        requireText(member, 100);
        if (coverage == null || authorization == null)
            throw new IllegalArgumentException("Coverage and authorization status are required");
        if (verifiedOn != null && verifiedOn.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Coverage check cannot be in the future");
        if (reference != null && reference.length() > 100)
            throw new IllegalArgumentException("Authorization reference too long");
        if (authorization == AuthorizationStatus.APPROVED) requireText(reference, 100);
        this.payerName = payer;
        this.planReference = plan;
        this.memberReference = member;
        this.coverageStatus = coverage;
        this.coverageVerifiedOn = verifiedOn;
        this.authorizationStatus = authorization;
        this.authorizationReference = reference;
    }

    private static void requireText(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException("Invalid insurance reference");
    }

    public String getPayerName() {
        return payerName;
    }

    public String getPlanReference() {
        return planReference;
    }

    public String getMemberReference() {
        return memberReference;
    }

    public CoverageStatus getCoverageStatus() {
        return coverageStatus;
    }

    public LocalDate getCoverageVerifiedOn() {
        return coverageVerifiedOn;
    }

    public AuthorizationStatus getAuthorizationStatus() {
        return authorizationStatus;
    }

    public String getAuthorizationReference() {
        return authorizationReference;
    }
}
