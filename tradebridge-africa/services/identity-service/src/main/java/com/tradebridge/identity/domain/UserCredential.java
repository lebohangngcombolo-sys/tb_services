package com.tradebridge.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Password credentials and lockout state (spec §3.2 {@code user_credentials}).
 *
 * <p>The password hash is produced by {@code PasswordEncoder} (Argon2 or
 * BCrypt) in the service layer. Never stores plaintext.
 */
@Entity
@Table(name = "user_credentials",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_credentials_user",
                columnNames = "user_id"))
public class UserCredential {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "password_expires_at")
    private Instant passwordExpiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserCredential() { }

    public UserCredential(UUID userId, String passwordHash) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "userId");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.failedAttempts = 0;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (passwordChangedAt == null) passwordChangedAt = now;
    }

    // ------------------------------------------------------------------ behavior

    public void updatePassword(String newHash) {
        this.passwordHash = Objects.requireNonNull(newHash);
        this.passwordChangedAt = Instant.now();
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public void recordFailedAttempt(int threshold, java.time.Duration lockDuration) {
        this.failedAttempts++;
        if (this.failedAttempts >= threshold) {
            this.lockedUntil = Instant.now().plus(lockDuration);
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public boolean isLockedAt(Instant when) {
        return lockedUntil != null && when.isBefore(lockedUntil);
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public Instant getPasswordExpiresAt() { return passwordExpiresAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
}
