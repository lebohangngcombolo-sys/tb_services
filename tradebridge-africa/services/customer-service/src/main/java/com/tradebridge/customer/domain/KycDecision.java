package com.tradebridge.customer.domain;

import com.tradebridge.customer.domain.enums.KycDecisionType;
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
 * KYC decision (spec §6.2 {@code kyc_decisions}).
 *
 * <p><b>Immutable.</b> Corrections create a new row with
 * {@code supersedesDecisionId} pointing at the previous row
 * (spec §6.4 "Decisions must be immutable and versioned"). Never update
 * an existing decision.
 */
@Entity
@Table(name = "kyc_decisions")
public class KycDecision {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "kyc_case_id", nullable = false, updatable = false)
    private UUID kycCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private KycDecisionType decision;

    @Column(name = "decision_reason_code", length = 100)
    private String decisionReasonCode;

    @Column(name = "decision_reason", columnDefinition = "text")
    private String decisionReason;

    @Column(name = "decided_by_user_id", nullable = false, updatable = false)
    private UUID decidedByUserId;

    @Column(name = "decision_version", nullable = false, updatable = false)
    private int decisionVersion;

    @Column(name = "supersedes_decision_id", updatable = false)
    private UUID supersedesDecisionId;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected KycDecision() { }

    public KycDecision(UUID id, UUID kycCaseId, KycDecisionType decision,
                        String decisionReasonCode, String decisionReason,
                        UUID decidedByUserId, int decisionVersion,
                        UUID supersedesDecisionId) {
        this.id = Objects.requireNonNull(id, "id");
        this.kycCaseId = Objects.requireNonNull(kycCaseId, "kycCaseId");
        this.decision = Objects.requireNonNull(decision, "decision");
        this.decisionReasonCode = decisionReasonCode;
        this.decisionReason = decisionReason;
        this.decidedByUserId = Objects.requireNonNull(decidedByUserId, "decidedByUserId");
        this.decisionVersion = decisionVersion;
        this.supersedesDecisionId = supersedesDecisionId;
    }

    @PrePersist
    void onCreate() {
        if (decidedAt == null) decidedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getKycCaseId() { return kycCaseId; }
    public KycDecisionType getDecision() { return decision; }
    public String getDecisionReasonCode() { return decisionReasonCode; }
    public String getDecisionReason() { return decisionReason; }
    public UUID getDecidedByUserId() { return decidedByUserId; }
    public int getDecisionVersion() { return decisionVersion; }
    public UUID getSupersedesDecisionId() { return supersedesDecisionId; }
    public Instant getDecidedAt() { return decidedAt; }
}