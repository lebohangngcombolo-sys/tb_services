package com.tradebridge.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Structured audit log (spec §5.2 {@code notification_logs}).
 *
 * <p>Never stores passwords, OTPs, tokens, or full message bodies (spec
 * §5.4). Only action names, results and non-sensitive metadata.
 */
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "result", nullable = false, length = 30)
    private String result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected NotificationLog() { }

    public NotificationLog(UUID id, UUID notificationId, UUID deliveryId,
                           String action, String result, Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "id");
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId");
        this.deliveryId = deliveryId;
        this.action = Objects.requireNonNull(action, "action");
        this.result = Objects.requireNonNull(result, "result");
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() {
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getNotificationId() { return notificationId; }
    public UUID getDeliveryId() { return deliveryId; }
    public String getAction() { return action; }
    public String getResult() { return result; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getOccurredAt() { return occurredAt; }
}