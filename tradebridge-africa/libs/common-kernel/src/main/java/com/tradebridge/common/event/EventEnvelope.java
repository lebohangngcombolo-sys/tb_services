package com.tradebridge.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical Kafka event envelope per spec §1.4:
 *
 * <pre>
 * {
 *   "eventId": "uuid",
 *   "eventType": "identity.user.registered.v1",
 *   "eventVersion": 1,
 *   "aggregateId": "uuid",
 *   "occurredAt": "2026-11-11T10:00:00Z",
 *   "producer": "identity-service",
 *   "correlationId": "uuid",
 *   "causationId": "uuid",
 *   "payload": {}
 * }
 * </pre>
 *
 * The message key for Kafka must be {@code aggregateId} when ordering matters.
 *
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public final class EventEnvelope<T> {

    @JsonProperty("eventId")
    private final UUID eventId;

    @JsonProperty("eventType")
    private final String eventType;

    @JsonProperty("eventVersion")
    private final int eventVersion;

    @JsonProperty("aggregateId")
    private final UUID aggregateId;

    @JsonProperty("occurredAt")
    private final Instant occurredAt;

    @JsonProperty("producer")
    private final String producer;

    @JsonProperty("correlationId")
    private final UUID correlationId;

    @JsonProperty("causationId")
    private final UUID causationId;

    @JsonProperty("payload")
    private final T payload;

    private EventEnvelope(UUID eventId, String eventType, int eventVersion, UUID aggregateId,
                          Instant occurredAt, String producer, UUID correlationId, UUID causationId, T payload) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.eventVersion = eventVersion;
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.producer = Objects.requireNonNull(producer, "producer");
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;

        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion must be >= 1");
        }
        if (!eventType.endsWith(".v" + eventVersion)) {
            throw new IllegalArgumentException(
                    "eventType '" + eventType + "' must end with '.v" + eventVersion + "'");
        }
    }

    public static <T> EventEnvelope<T> of(EventMetadata metadata, T payload) {
        return new EventEnvelope<>(
                metadata.eventId(),
                metadata.eventType(),
                metadata.eventVersion(),
                metadata.aggregateId(),
                metadata.occurredAt(),
                metadata.producer(),
                metadata.correlationId(),
                metadata.causationId(),
                payload);
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public int getEventVersion() { return eventVersion; }
    public UUID getAggregateId() { return aggregateId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getProducer() { return producer; }
    public UUID getCorrelationId() { return correlationId; }
    public UUID getCausationId() { return causationId; }
    public T getPayload() { return payload; }
}