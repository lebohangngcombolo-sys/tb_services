package com.tradebridge.customer.domain;

import com.tradebridge.customer.domain.enums.ControlType;
import com.tradebridge.customer.domain.enums.RepresentativeVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Beneficial owner (spec §6.2 {@code beneficial_owners}).
 *
 * <p>{@code ownershipPercentage} may be null for control-only owners
 * (spec §6.4 "Control relationships must be recorded even where ownership
 * percentage is unavailable").
 *
 * <p>{@code identityReferenceHash} is a hash of a government identifier
 * (SA ID, passport number). Raw values are not stored.
 */
@Entity
@Table(name = "beneficial_owners")
public class BeneficialOwner {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality_country_code", length = 2)
    private String nationalityCountryCode;

    @Column(name = "residence_country_code", length = 2)
    private String residenceCountryCode;

    @Column(name = "ownership_percentage", precision = 7, scale = 4)
    private BigDecimal ownershipPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 40)
    private ControlType controlType;

    @Column(name = "identity_reference_hash", length = 255)
    private String identityReferenceHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private RepresentativeVerificationStatus verificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BeneficialOwner() { }

    public BeneficialOwner(UUID id, UUID organizationId, String firstName, String lastName,
                            LocalDate dateOfBirth, String nationalityCountryCode,
                            String residenceCountryCode, BigDecimal ownershipPercentage,
                            ControlType controlType, String identityReferenceHash) {
        this.id = Objects.requireNonNull(id, "id");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.dateOfBirth = dateOfBirth;
        this.nationalityCountryCode = nationalityCountryCode == null ? null : nationalityCountryCode.toUpperCase();
        this.residenceCountryCode = residenceCountryCode == null ? null : residenceCountryCode.toUpperCase();
        this.ownershipPercentage = ownershipPercentage;
        this.controlType = Objects.requireNonNull(controlType, "controlType");
        this.identityReferenceHash = identityReferenceHash;
        this.verificationStatus = RepresentativeVerificationStatus.PENDING;
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

    public void updateDetails(String firstName, String lastName, LocalDate dateOfBirth,
                              String nationalityCountryCode, String residenceCountryCode,
                              BigDecimal ownershipPercentage, ControlType controlType,
                              String identityReferenceHash) {
        if (firstName != null && !firstName.isBlank()) this.firstName = firstName;
        if (lastName != null && !lastName.isBlank()) this.lastName = lastName;
        if (dateOfBirth != null) this.dateOfBirth = dateOfBirth;
        if (nationalityCountryCode != null) this.nationalityCountryCode = nationalityCountryCode.toUpperCase();
        if (residenceCountryCode != null) this.residenceCountryCode = residenceCountryCode.toUpperCase();
        if (ownershipPercentage != null) this.ownershipPercentage = ownershipPercentage;
        if (controlType != null) this.controlType = controlType;
        if (identityReferenceHash != null) this.identityReferenceHash = identityReferenceHash;
    }

    public void verify() {
        this.verificationStatus = RepresentativeVerificationStatus.VERIFIED;
    }

    public void reject() {
        this.verificationStatus = RepresentativeVerificationStatus.REJECTED;
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getNationalityCountryCode() { return nationalityCountryCode; }
    public String getResidenceCountryCode() { return residenceCountryCode; }
    public BigDecimal getOwnershipPercentage() { return ownershipPercentage; }
    public ControlType getControlType() { return controlType; }
    public String getIdentityReferenceHash() { return identityReferenceHash; }
    public RepresentativeVerificationStatus getVerificationStatus() { return verificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}