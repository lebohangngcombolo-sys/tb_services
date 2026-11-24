package com.tradebridge.country.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Country-specific organization onboarding requirement (spec §4.2
 * {@code organization_requirements}).
 *
 * <p>Country Service owns the *rules*; Customer Service owns the actual
 * onboarding cases (spec §4.4 "Country Service stores onboarding
 * requirements, not KYC decisions").
 */
@Entity
@Table(name = "organization_requirements",
        uniqueConstraints = @UniqueConstraint(name = "uk_org_requirements",
                columnNames = {"country_code", "organization_type", "requirement_code"}))
public class OrganizationRequirement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "organization_type", nullable = false, length = 50)
    private String organizationType;

    @Column(name = "requirement_code", nullable = false, length = 100)
    private String requirementCode;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "required", nullable = false)
    private boolean required;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rule", columnDefinition = "jsonb")
    private Map<String, Object> validationRule;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected OrganizationRequirement() { }

    public OrganizationRequirement(UUID id, String countryCode, String organizationType,
                                   String requirementCode, String description, boolean required,
                                   Map<String, Object> validationRule,
                                   Instant effectiveFrom, Instant effectiveUntil, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.organizationType = Objects.requireNonNull(organizationType, "organizationType");
        this.requirementCode = Objects.requireNonNull(requirementCode, "requirementCode");
        this.description = description;
        this.required = required;
        this.validationRule = validationRule;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        // no timestamps on this table per spec — effective_from/until cover temporal concerns
    }

    public void updateDetails(String description, Boolean required, Map<String, Object> validationRule,
                              Instant effectiveFrom, Instant effectiveUntil) {
        if (description != null) this.description = description;
        if (required != null) this.required = required;
        if (validationRule != null) this.validationRule = validationRule;
        if (effectiveFrom != null) this.effectiveFrom = effectiveFrom;
        if (effectiveUntil != null) this.effectiveUntil = effectiveUntil;
    }

    public void setEnabled(boolean v) { this.enabled = v; }

    public UUID getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public String getOrganizationType() { return organizationType; }
    public String getRequirementCode() { return requirementCode; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
    public Map<String, Object> getValidationRule() { return validationRule; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveUntil() { return effectiveUntil; }
    public boolean isEnabled() { return enabled; }
    public long getRowVersion() { return rowVersion; }
}