package com.tradebridge.identity.domain;

import com.tradebridge.identity.domain.enums.ChallengeType;
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
 * Short-lived challenge for email verification, password reset and MFA
 * email OTP (spec §3.2 {@code verification_challenges}).
 *
 * <p>The OTP hash is stored; the raw code is delivered through the
 * notification handoff mechanism (spec §3.3), never persisted.
 */
@Entity
@Table(name = "verification_challenges")
public class VerificationChallenge {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 40)
    private ChallengeType challengeType;

    @Column(name = "destination_masked", length = 255)
    private String destinationMasked;

    @Column(name = "code_hash", length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationChallenge() { }

    public VerificationChallenge(UUID userId, ChallengeType challengeType,
                                 String destinationMasked, String codeHash, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "userId");
        this.challengeType = Objects.requireNonNull(challengeType, "challengeType");
        this.destinationMasked = destinationMasked;
        this.codeHash = codeHash;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.attemptCount = 0;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isUsableAt(Instant when, int maxAttempts) {
        return consumedAt == null
                && when.isBefore(expiresAt)
                && attemptCount < maxAttempts;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public void consume() {
        if (consumedAt == null) {
            this.consumedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public ChallengeType getChallengeType() { return challengeType; }
    public String getDestinationMasked() { return destinationMasked; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
