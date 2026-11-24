package com.tradebridge.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base mapped class for service-specific outbox tables.
 *
 * <p>Every producer service must declare a concrete subclass, e.g.
 * {@code IdentityOutboxEntry extends OutboxEntry}, mapped to a table named
 * {@code identity_outbox}, and declare a repository over it. The starter
 * relays via the {@link org.springframework.data.jpa.repository.JpaRepository}
 * for that subclass.
 */
@MappedSuperclass
public abstract class OutboxEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected OutboxEntry() { }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = com.tradebridge.common.id.UuidV7Generator.next();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
    }

    // ------------------------------------------------------------------ builders

    public void markPending(UUID aggregateId, UUID eventId, String eventType, String topic, String payload) {
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.status = OutboxStatus.PENDING;
    }

    public void markPublished(Instant when) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = when;
        this.lastError = null;
    }

    public void markFailed(String reason) {
        this.status = OutboxStatus.FAILED;
        this.lastError = truncate(reason, 4000);
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public long getRowVersion() { return rowVersion; }
}
