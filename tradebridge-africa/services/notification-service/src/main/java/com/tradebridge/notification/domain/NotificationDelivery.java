package com.tradebridge.notification.domain;

import com.tradebridge.notification.domain.enums.DeliveryStatus;
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
 * Per-attempt delivery record (spec §5.2 {@code notification_deliveries}).
 *
 * <p>Each send attempt creates a row. Retry creates new rows; history is
 * preserved for audit and postmortem.
 */
@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationDelivery() { }

    public NotificationDelivery(UUID id, UUID notificationId, String provider,
                                int attemptNumber, DeliveryStatus status,
                                Instant submittedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.attemptNumber = attemptNumber;
        this.status = Objects.requireNonNull(status, "status");
        this.submittedAt = submittedAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void markAccepted(String providerMessageId, Instant submittedAt) {
        this.status = DeliveryStatus.ACCEPTED;
        this.providerMessageId = providerMessageId;
        this.submittedAt = submittedAt;
    }

    public void markDelivered(Instant when) {
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = when;
    }

    public void markBounced(String reason) {
        this.status = DeliveryStatus.BOUNCED;
        this.failureReason = reason;
    }

    public void markFailed(String code, String reason) {
        this.status = DeliveryStatus.FAILED;
        this.failureCode = code;
        this.failureReason = reason;
    }

    public UUID getId() { return id; }
    public UUID getNotificationId() { return notificationId; }
    public String getProvider() { return provider; }
    public String getProviderMessageId() { return providerMessageId; }
    public DeliveryStatus getStatus() { return status; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getFailureCode() { return failureCode; }
    public String getFailureReason() { return failureReason; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getCreatedAt() { return createdAt; }
}