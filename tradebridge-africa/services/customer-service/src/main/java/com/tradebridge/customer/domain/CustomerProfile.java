package com.tradebridge.customer.domain;

import com.tradebridge.customer.domain.enums.CustomerStatus;
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
import java.util.Objects;
import java.util.UUID;

/**
 * Customer profile aggregate (spec §6.2 {@code customer_profiles}).
 *
 * <p>Logical reference to Identity's user via {@code identity_user_id} —
 * no cross-service FK, per spec §1.3.
 */
@Entity
@Table(name = "customer_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_profiles_identity", columnNames = "identity_user_id"),
                @UniqueConstraint(name = "uk_customer_profiles_number", columnNames = "customer_number")
        })
public class CustomerProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "identity_user_id", nullable = false, updatable = false)
    private UUID identityUserId;

    @Column(name = "customer_number", length = 40)
    private String customerNumber;

    @Column(name = "customer_type", nullable = false, length = 30)
    private String customerType;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "preferred_language", nullable = false, length = 5)
    private String preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CustomerStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected CustomerProfile() { }

    public CustomerProfile(UUID id, UUID identityUserId, String customerType,
                           String countryCode, String preferredLanguage) {
        this.id = Objects.requireNonNull(id, "id");
        this.identityUserId = Objects.requireNonNull(identityUserId, "identityUserId");
        this.customerType = Objects.requireNonNull(customerType, "customerType");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.preferredLanguage = Objects.requireNonNull(preferredLanguage, "preferredLanguage").toLowerCase();
        this.status = CustomerStatus.PENDING;
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

    public void assignCustomerNumber(String number) {
        if (this.customerNumber != null) return;
        this.customerNumber = Objects.requireNonNull(number);
    }

    public void activate() {
        if (status != CustomerStatus.CLOSED) this.status = CustomerStatus.ACTIVE;
    }

    public void suspend() {
        if (status != CustomerStatus.CLOSED) this.status = CustomerStatus.SUSPENDED;
    }

    public void close() {
        this.status = CustomerStatus.CLOSED;
    }

    public void updatePreferences(String preferredLanguage) {
        if (preferredLanguage != null && !preferredLanguage.isBlank()) {
            this.preferredLanguage = preferredLanguage.toLowerCase();
        }
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getIdentityUserId() { return identityUserId; }
    public String getCustomerNumber() { return customerNumber; }
    public String getCustomerType() { return customerType; }
    public String getCountryCode() { return countryCode; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public CustomerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getRowVersion() { return rowVersion; }
}