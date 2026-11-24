package com.tradebridge.customer.domain;

import com.tradebridge.customer.domain.enums.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Organization aggregate (spec §6.2 {@code organizations}).
 *
 * <p>Owns addresses, representatives, beneficial owners, and KYC cases.
 * The {@code created_by_user_id} is an Identity user ID (logical reference).
 */
@Entity
@Table(name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_organizations_number", columnNames = "organization_number"),
                @UniqueConstraint(name = "uk_organizations_registration",
                        columnNames = {"country_code", "registration_authority", "registration_number"})
        })
public class Organization {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_number", length = 50)
    private String organizationNumber;

    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    @Column(name = "trading_name", length = 255)
    private String tradingName;

    @Column(name = "organization_type", nullable = false, length = 50)
    private String organizationType;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    @Column(name = "registration_authority", length = 150)
    private String registrationAuthority;

    @Column(name = "business_description", columnDefinition = "text")
    private String businessDescription;

    @Column(name = "website", length = 255)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrganizationStatus status;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected Organization() { }

    public Organization(UUID id, String legalName, String organizationType,
                        String registrationNumber, String countryCode,
                        UUID createdByUserId) {
        this.id = Objects.requireNonNull(id, "id");
        this.legalName = Objects.requireNonNull(legalName, "legalName");
        this.organizationType = Objects.requireNonNull(organizationType, "organizationType");
        this.registrationNumber = Objects.requireNonNull(registrationNumber, "registrationNumber");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.status = OrganizationStatus.DRAFT;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------ behavior

    public void assignOrganizationNumber(String number) {
        if (this.organizationNumber != null) return;
        this.organizationNumber = Objects.requireNonNull(number);
    }

    public void updateDetails(String legalName, String tradingName, String taxNumber,
                              LocalDate incorporationDate, String registrationAuthority,
                              String businessDescription, String website) {
        if (legalName != null && !legalName.isBlank()) this.legalName = legalName;
        if (tradingName != null) this.tradingName = tradingName;
        if (taxNumber != null) this.taxNumber = taxNumber;
        if (incorporationDate != null) this.incorporationDate = incorporationDate;
        if (registrationAuthority != null) this.registrationAuthority = registrationAuthority;
        if (businessDescription != null) this.businessDescription = businessDescription;
        if (website != null) this.website = website;
    }

    public void markSubmitted() {
        this.status = OrganizationStatus.PENDING_REVIEW;
    }

    public void markApproved() {
        this.status = OrganizationStatus.APPROVED;
    }

    public void markRejected() {
        this.status = OrganizationStatus.REJECTED;
    }

    public void markWithdrawn() {
        this.status = OrganizationStatus.WITHDRAWN;
    }

    public void resetToDraft() {
        this.status = OrganizationStatus.DRAFT;
    }

    public boolean canBeEdited() {
        return status.canBeEdited();
    }

    public boolean canBeSubmitted() {
        return status.canBeSubmitted();
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public String getOrganizationNumber() { return organizationNumber; }
    public String getLegalName() { return legalName; }
    public String getTradingName() { return tradingName; }
    public String getOrganizationType() { return organizationType; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getTaxNumber() { return taxNumber; }
    public String getCountryCode() { return countryCode; }
    public LocalDate getIncorporationDate() { return incorporationDate; }
    public String getRegistrationAuthority() { return registrationAuthority; }
    public String getBusinessDescription() { return businessDescription; }
    public String getWebsite() { return website; }
    public OrganizationStatus getStatus() { return status; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getRowVersion() { return rowVersion; }
}