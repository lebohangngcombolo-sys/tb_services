package com.tradebridge.identity.domain;

import com.tradebridge.identity.domain.enums.UserStatus;
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
 * Core user identity (spec §3.2 {@code users}).
 *
 * <p>Does not carry profile data beyond what is needed to authenticate and
 * contact the user — the Customer Service owns customer profiles.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_phone", columnNames = "phone_number")
        })
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "preferred_language", nullable = false, length = 5)
    private String preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected User() { }

    public User(UUID id, String firstName, String lastName, String email,
                String phoneNumber, String countryCode, String preferredLanguage) {
        this.id = Objects.requireNonNull(id, "id");
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.email = Objects.requireNonNull(email, "email");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode");
        this.preferredLanguage = Objects.requireNonNull(preferredLanguage, "preferredLanguage");
        this.status = UserStatus.PENDING_VERIFICATION;
        this.emailVerified = false;
        this.phoneVerified = false;
        this.mfaEnabled = false;
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

    public void markEmailVerified() {
        if (emailVerified) return;
        this.emailVerified = true;
    }

    public void markPhoneVerified() {
        if (phoneVerified) return;
        this.phoneVerified = true;
    }

    public void activate() {
        if (status == UserStatus.ACTIVE) return;
        this.status = UserStatus.ACTIVE;
    }

    public void suspend() {
        if (status == UserStatus.CLOSED) return;
        this.status = UserStatus.SUSPENDED;
    }

    public void lock() {
        if (status == UserStatus.CLOSED) return;
        this.status = UserStatus.LOCKED;
    }

    public void unlock() {
        if (status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public void close() {
        this.status = UserStatus.CLOSED;
    }

    public void setMfaEnabled(boolean enabled) {
        this.mfaEnabled = enabled;
    }

    public void updateProfileName(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getCountryCode() { return countryCode; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public UserStatus getStatus() { return status; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getRowVersion() { return rowVersion; }
}
