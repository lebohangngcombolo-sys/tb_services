package com.tradebridge.customer.domain;

import com.tradebridge.customer.domain.enums.RepresentativeType;
import com.tradebridge.customer.domain.enums.RepresentativeVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Organization representative (spec §6.2 {@code organization_representatives}).
 *
 * <p>{@code identityUserId} is optional — a representative can be recorded
 * before their identity is created (e.g. a foreign director).
 */
@Entity
@Table(name = "organization_representatives")
public class OrganizationRepresentative {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "identity_user_id")
    private UUID identityUserId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "representative_type", nullable = false, length = 40)
    private RepresentativeType representativeType;

    @Column(name = "is_authorized_signatory", nullable = false)
    private boolean authorizedSignatory;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private RepresentativeVerificationStatus verificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrganizationRepresentative() { }

    public OrganizationRepresentative(UUID id, UUID organizationId, UUID identityUserId,
                                       String firstName, String lastName, String email,
                                       String phoneNumber, RepresentativeType representativeType,
                                       boolean authorizedSignatory) {
        this.id = Objects.requireNonNull(id, "id");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.identityUserId = identityUserId;
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.email = Objects.requireNonNull(email, "email").toLowerCase();
        this.phoneNumber = phoneNumber;
        this.representativeType = Objects.requireNonNull(representativeType, "representativeType");
        this.authorizedSignatory = authorizedSignatory;
        this.verificationStatus = RepresentativeVerificationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // ------------------------------------------------------------------ behavior

    public void updateDetails(String firstName, String lastName, String email,
                              String phoneNumber, RepresentativeType representativeType,
                              Boolean authorizedSignatory) {
        if (firstName != null && !firstName.isBlank()) this.firstName = firstName;
        if (lastName != null && !lastName.isBlank()) this.lastName = lastName;
        if (email != null && !email.isBlank()) this.email = email.toLowerCase();
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (representativeType != null) this.representativeType = representativeType;
        if (authorizedSignatory != null) this.authorizedSignatory = authorizedSignatory;
    }

    public void verify() {
        this.verificationStatus = RepresentativeVerificationStatus.VERIFIED;
    }

    public void reject() {
        this.verificationStatus = RepresentativeVerificationStatus.REJECTED;
    }

    public void resetVerification() {
        this.verificationStatus = RepresentativeVerificationStatus.PENDING;
    }

    public void revokeAuthorization() {
        this.authorizedSignatory = false;
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getIdentityUserId() { return identityUserId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public RepresentativeType getRepresentativeType() { return representativeType; }
    public boolean isAuthorizedSignatory() { return authorizedSignatory; }
    public RepresentativeVerificationStatus getVerificationStatus() { return verificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
}