package com.tradebridge.notification.domain;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.common.enums.Priority;
import com.tradebridge.notification.domain.enums.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A notification record (spec §5.2 {@code notifications}).
 *
 * <p>Deliberately does not persist the raw rendered body. The subject is
 * stored (needed for the customer-facing notification centre); the body
 * is stored as {@code body_reference} pointing to secured storage (MinIO)
 * when message history is required, or as null when not needed.
 *
 * <p>OTP messages never store body content — only the masked recipient
 * lives in {@code notification_recipients}.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    @Column(name = "recipient_reference", length = 255)
    private String recipientReference;

    @Column(name = "template_code", length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    @Column(name = "subject", columnDefinition = "text")
    private String subject;

    @Column(name = "body_reference")
    private UUID bodyReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "idempotency_key", length = 150)
    private String idempotencyKey;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected Notification() { }

    public Notification(UUID id, UUID recipientUserId, String recipientReference,
                        String templateCode, Channel channel, String languageCode,
                        String subject, NotificationStatus status, Priority priority,
                        String sourceService, UUID correlationId, String idempotencyKey,
                        Instant scheduledAt, Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.recipientUserId = recipientUserId;
        this.recipientReference = recipientReference;
        this.templateCode = templateCode;
        this.channel = Objects.requireNonNull(channel, "channel");
        this.languageCode = Objects.requireNonNull(languageCode, "languageCode").toLowerCase();
        this.subject = subject;
        this.status = Objects.requireNonNull(status, "status");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.sourceService = Objects.requireNonNull(sourceService, "sourceService");
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.scheduledAt = scheduledAt;
        this.expiresAt = expiresAt;
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

    // ------------------------------------------------------------------ lifecycle

    public void markSent() {
        this.status = NotificationStatus.SENT;
    }

    public void markDelivered() {
        this.status = NotificationStatus.DELIVERED;
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public void markExhausted() {
        this.status = NotificationStatus.EXHAUSTED;
    }

    public void markScheduled(Instant when) {
        this.status = NotificationStatus.SCHEDULED;
        this.scheduledAt = when;
    }

    public void setBodyReference(UUID bodyReference) {
        this.bodyReference = bodyReference;
    }

    public boolean isExpiredAt(Instant when) {
        return expiresAt != null && when.isAfter(expiresAt);
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getRecipientUserId() { return recipientUserId; }
    public String getRecipientReference() { return recipientReference; }
    public String getTemplateCode() { return templateCode; }
    public Channel getChannel() { return channel; }
    public String getLanguageCode() { return languageCode; }
    public String getSubject() { return subject; }
    public UUID getBodyReference() { return bodyReference; }
    public NotificationStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public String getSourceService() { return sourceService; }
    public UUID getCorrelationId() { return correlationId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}