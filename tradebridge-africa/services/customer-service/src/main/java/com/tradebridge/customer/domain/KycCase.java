package com.tradebridge.customer.domain;

import com.tradebridge.customer.domain.enums.KycCaseStatus;
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
 * KYC case (spec §6.2 {@code kyc_cases}).
 *
 * <p>{@code countryConfigurationVersion} records which country configuration
 * was used when the case was created (spec §11 decision 3). This guarantees
 * the reviewer knows exactly what rules were in force.
 */
@Entity
@Table(name = "kyc_cases",
        uniqueConstraints = @UniqueConstraint(name = "uk_kyc_cases_number",
                columnNames = "case_number"))
public class KycCase {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "case_number", length = 50)
    private String caseNumber;

    @Column(name = "case_type", nullable = false, length = 30)
    private String caseType;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "country_configuration_version", nullable = false)
    private int countryConfigurationVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private KycCaseStatus status;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "assigned_reviewer_id")
    private UUID assignedReviewerId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected KycCase() { }

    public KycCase(UUID id, UUID organizationId, String caseType,
                   String countryCode, int countryConfigurationVersion) {
        this.id = Objects.requireNonNull(id, "id");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.caseType = Objects.requireNonNull(caseType, "caseType");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.countryConfigurationVersion = countryConfigurationVersion;
        this.status = KycCaseStatus.DRAFT;
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

    public void assignCaseNumber(String number) {
        if (this.caseNumber != null) return;
        this.caseNumber = Objects.requireNonNull(number);
    }

    public void submit() {
        this.status = KycCaseStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void startReview(UUID reviewerId, String riskLevel) {
        if (this.status != KycCaseStatus.SUBMITTED
                && this.status != KycCaseStatus.RESUBMITTED) {
            throw new IllegalStateException("Cannot start review in status " + status);
        }
        this.status = KycCaseStatus.IN_REVIEW;
        this.assignedReviewerId = reviewerId;
        if (riskLevel != null) this.riskLevel = riskLevel;
    }

    public void assignReviewer(UUID reviewerId) {
        this.assignedReviewerId = reviewerId;
    }

    public void requestInformation() {
        this.status = KycCaseStatus.INFORMATION_REQUESTED;
    }

    public void resubmit() {
        if (this.status != KycCaseStatus.INFORMATION_REQUESTED) {
            throw new IllegalStateException("Cannot resubmit in status " + status);
        }
        this.status = KycCaseStatus.RESUBMITTED;
    }

    public void approve() {
        this.status = KycCaseStatus.APPROVED;
        this.completedAt = Instant.now();
    }

    public void reject() {
        this.status = KycCaseStatus.REJECTED;
        this.completedAt = Instant.now();
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getCaseNumber() { return caseNumber; }
    public String getCaseType() { return caseType; }
    public String getCountryCode() { return countryCode; }
    public int getCountryConfigurationVersion() { return countryConfigurationVersion; }
    public KycCaseStatus getStatus() { return status; }
    public String getRiskLevel() { return riskLevel; }
    public UUID getAssignedReviewerId() { return assignedReviewerId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getRowVersion() { return rowVersion; }
}