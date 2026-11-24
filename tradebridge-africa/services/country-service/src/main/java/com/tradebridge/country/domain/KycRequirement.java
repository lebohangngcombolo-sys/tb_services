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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Country-specific KYC document requirement (spec §4.2
 * {@code kyc_requirements}).
 *
 * <p>Defines *what* evidence is required, not the outcome of any
 * particular customer's review.
 */
@Entity
@Table(name = "kyc_requirements",
        uniqueConstraints = @UniqueConstraint(name = "uk_kyc_requirements",
                columnNames = {"country_code", "customer_type", "requirement_code"}))
public class KycRequirement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "customer_type", nullable = false, length = 30)
    private String customerType;

    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(name = "requirement_code", nullable = false, length = 100)
    private String requirementCode;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "required", nullable = false)
    private boolean required;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_mime_types", columnDefinition = "jsonb")
    private List<String> allowedMimeTypes;

    @Column(name = "max_file_size_bytes")
    private Long maxFileSizeBytes;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected KycRequirement() { }

    public KycRequirement(UUID id, String countryCode, String customerType, String documentType,
                          String requirementCode, String description, boolean required,
                          List<String> allowedMimeTypes, Long maxFileSizeBytes,
                          Integer validityDays, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.customerType = Objects.requireNonNull(customerType, "customerType");
        this.documentType = Objects.requireNonNull(documentType, "documentType");
        this.requirementCode = Objects.requireNonNull(requirementCode, "requirementCode");
        this.description = description;
        this.required = required;
        this.allowedMimeTypes = allowedMimeTypes;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.validityDays = validityDays;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        // effective window not modeled here per spec — validity_days covers expiry
    }

    public void updateDetails(String description, Boolean required, List<String> allowedMimeTypes,
                              Long maxFileSizeBytes, Integer validityDays) {
        if (description != null) this.description = description;
        if (required != null) this.required = required;
        if (allowedMimeTypes != null) this.allowedMimeTypes = allowedMimeTypes;
        if (maxFileSizeBytes != null) this.maxFileSizeBytes = maxFileSizeBytes;
        if (validityDays != null) this.validityDays = validityDays;
    }

    public void setEnabled(boolean v) { this.enabled = v; }

    public UUID getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public String getCustomerType() { return customerType; }
    public String getDocumentType() { return documentType; }
    public String getRequirementCode() { return requirementCode; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
    public List<String> getAllowedMimeTypes() { return allowedMimeTypes; }
    public Long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public Integer getValidityDays() { return validityDays; }
    public boolean isEnabled() { return enabled; }
    public long getRowVersion() { return rowVersion; }
}