package com.claimiq.backend.claims.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Embeddable
public class ClaimServiceLine {
    public enum CodeSystem {
        LOCAL,
        CPT,
        HCPCS
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "code_system", nullable = false, length = 20)
    private CodeSystem codeSystem;

    @Column(name = "procedure_code", nullable = false, length = 20)
    private String procedureCode;

    @Nationalized
    @Column(name = "service_description", nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private int units;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    protected ClaimServiceLine() {}

    public ClaimServiceLine(
            CodeSystem system, String code, String description, int units, BigDecimal unitPrice) {
        if (system == null || code == null || !code.matches("[A-Z0-9][A-Z0-9.-]{0,19}"))
            throw new IllegalArgumentException("Invalid procedure code");
        if (description == null || description.isBlank() || description.length() > 200)
            throw new IllegalArgumentException("Service description is required");
        if (units < 1 || units > 999)
            throw new IllegalArgumentException("Units must be between 1 and 999");
        if (unitPrice == null
                || unitPrice.signum() <= 0
                || unitPrice.scale() > 2
                || unitPrice.precision() - unitPrice.scale() > 16)
            throw new IllegalArgumentException("Invalid unit price");
        this.codeSystem = system;
        this.procedureCode = code;
        this.description = description;
        this.units = units;
        this.unitPrice = unitPrice;
    }

    public CodeSystem getCodeSystem() {
        return codeSystem;
    }

    public String getProcedureCode() {
        return procedureCode;
    }

    public String getDescription() {
        return description;
    }

    public int getUnits() {
        return units;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal total() {
        return unitPrice.multiply(BigDecimal.valueOf(units));
    }
}
