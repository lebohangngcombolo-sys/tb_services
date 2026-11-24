package com.tradebridge.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * KYC review record (spec §6.2 {@code kyc_reviews}).
 *
 * <p>One row per review stage (INITIAL, SECONDARY, ENHANCED). Completing
 * a review transitions its {@code review_status} to COMPLETED and sets
 * {@code completed_at}.
 */
@Entity
@Table(name = "kyc_reviews")
public class KycReview {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "kyc_case_id", nullable = false, updatable = false)
    private UUID kycCaseId;

    @Column(name = "reviewer_user_id", nullable = false)
    private UUID reviewerUserId;

    @Column(name = "review_stage", nullable = false, length = 30)
    private String reviewStage;

    @Column(name = "review_status", nullable = false, length = 30)
    private String reviewStatus;

    @Column(name = "review_notes", columnDefinition = "text")
    private String reviewNotes;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected KycReview() { }

    public KycReview(UUID id, UUID kycCaseId, UUID reviewerUserId,
                     String reviewStage, String reviewNotes) {
        this.id = Objects.requireNonNull(id, "id");
        this.kycCaseId = Objects.requireNonNull(kycCaseId, "kycCaseId");
        this.reviewerUserId = Objects.requireNonNull(reviewerUserId, "reviewerUserId");
        this.reviewStage = Objects.requireNonNull(reviewStage, "reviewStage");
        this.reviewNotes = reviewNotes;
        this.reviewStatus = "IN_PROGRESS";
        this.startedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void complete(String outcome, String notes) {
        this.reviewStatus = "COMPLETED";
        this.completedAt = Instant.now();
        if (notes != null) this.reviewNotes = notes;
        // Outcome carried on the review record for audit — the actual
        // KYC decision lives in `kyc_decisions`.
        this.reviewNotes = (this.reviewNotes == null ? "" : this.reviewNotes)
                + " [outcome=" + outcome + "]";
    }

    public UUID getId() { return id; }
    public UUID getKycCaseId() { return kycCaseId; }
    public UUID getReviewerUserId() { return reviewerUserId; }
    public String getReviewStage() { return reviewStage; }
    public String getReviewStatus() { return reviewStatus; }
    public String getReviewNotes() { return reviewNotes; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}