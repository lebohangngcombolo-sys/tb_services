package com.tradebridge.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Kafka wire-format envelope, aligned with spec §1.4. Mirrors
 * {@code common-kernel}'s {@code EventEnvelope} but is decoupled so that
 * the messaging module can evolve the wire format independently (e.g. add
 * tracing fields) without touching the API-visible record.
 *
 * <p>Kafka message key MUST be the {@code aggregateId} when ordering matters.
 */
public final class EventEnvelope {

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
    private final Object payload;

    @JsonCreator
    public EventEnvelope(@JsonProperty("eventId") UUID eventId,
                         @JsonProperty("eventType") String eventType,
                         @JsonProperty("eventVersion") int eventVersion,
                         @JsonProperty("aggregateId") UUID aggregateId,
                         @JsonProperty("occurredAt") Instant occurredAt,
                         @JsonProperty("producer") String producer,
                         @JsonProperty("correlationId") UUID correlationId,
                         @JsonProperty("causationId") UUID causationId,
                         @JsonProperty("payload") Object payload) {
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

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public int getEventVersion() { return eventVersion; }
    public UUID getAggregateId() { return aggregateId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getProducer() { return producer; }
    public UUID getCorrelationId() { return correlationId; }
    public UUID getCausationId() { return causationId; }
    public Object getPayload() { return payload; }
}
