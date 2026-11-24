package com.tradebridge.identity.domain;

import com.tradebridge.identity.domain.enums.MfaFactorType;
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
 * MFA factor per user (spec §3.2 {@code mfa_factors}).
 *
 * <p>The TOTP secret is stored encrypted at rest (application-managed
 * encryption key). Never log the plaintext secret.
 */
@Entity
@Table(name = "mfa_factors")
public class MfaFactor {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "factor_type", nullable = false, length = 30)
    private MfaFactorType factorType;

    @Column(name = "secret_encrypted", columnDefinition = "text")
    private String secretEncrypted;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected MfaFactor() { }

    public MfaFactor(UUID userId, MfaFactorType factorType, String secretEncrypted) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "userId");
        this.factorType = Objects.requireNonNull(factorType, "factorType");
        this.secretEncrypted = secretEncrypted;
        this.verified = false;
        this.enabled = false;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void markVerifiedAndEnabled(String encryptedSecret) {
        this.secretEncrypted = Objects.requireNonNull(encryptedSecret);
        this.verified = true;
        this.enabled = true;
    }

    public void replaceSecretForSetup(String encryptedSecret) {
        this.secretEncrypted = Objects.requireNonNull(encryptedSecret);
        this.verified = false;
        this.enabled = false;
    }

    public void disable() {
        this.enabled = false;
    }

    public void recordUsage() {
        this.lastUsedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public MfaFactorType getFactorType() { return factorType; }
    public String getSecretEncrypted() { return secretEncrypted; }
    public boolean isVerified() { return verified; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}
