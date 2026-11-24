package com.tradebridge.messaging.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base mapped class for service-specific inbox tables.
 *
 * <p>Consumers must record every processed event here in the same
 * transaction as the side effect, so redelivery is a no-op. Spec §1.4:
 * "Every consumer must be idempotent."
 *
 * <p>Unique constraint required on {@code (event_id, consumer_name)}.
 */
@MappedSuperclass
public abstract class InboxEntry {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, updatable = false, length = 100)
    private String consumerName;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @Column(name = "result", nullable = false, length = 30)
    private String result;

    protected InboxEntry() { }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = com.tradebridge.common.id.UuidV7Generator.next();
        }
        if (processedAt == null) {
            processedAt = Instant.now();
        }
        if (result == null) {
            result = "OK";
        }
    }

    public void initialize(UUID eventId, String consumerName, String result) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.consumerName = Objects.requireNonNull(consumerName, "consumerName");
        this.result = result == null ? "OK" : result;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getConsumerName() { return consumerName; }
    public Instant getProcessedAt() { return processedAt; }
    public String getResult() { return result; }
}
