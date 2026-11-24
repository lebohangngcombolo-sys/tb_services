package com.tradebridge.common.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Envelope metadata defined in spec §1.4 (excluding payload).
 */
public record EventMetadata(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID aggregateId,
        Instant occurredAt,
        String producer,
        UUID correlationId,
        UUID causationId
) {
    public EventMetadata {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(producer, "producer");
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion must be >= 1");
        }
        if (!eventType.endsWith(".v" + eventVersion)) {
            throw new IllegalArgumentException(
                    "eventType '" + eventType + "' must end with '.v" + eventVersion + "'");
        }
    }
}