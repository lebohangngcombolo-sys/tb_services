package com.tradebridge.messaging.serializer;

import com.tradebridge.messaging.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonEventDeserializerTest {

    @Test
    void deserializesEventEnvelopeWireFormat() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-11-23T13:36:18Z");
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "identity.user.registered.v1",
                  "eventVersion": 1,
                  "aggregateId": "%s",
                  "occurredAt": "%s",
                  "producer": "identity-service",
                  "correlationId": "%s",
                  "causationId": null,
                  "payload": {"userId": "user-123"}
                }
                """.formatted(eventId, aggregateId, occurredAt, correlationId);

        EventEnvelope envelope;
        try (JsonEventDeserializer deserializer = new JsonEventDeserializer()) {
            envelope = deserializer.deserialize("identity.events", json.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(eventId, envelope.getEventId());
        assertEquals("identity.user.registered.v1", envelope.getEventType());
        assertEquals(1, envelope.getEventVersion());
        assertEquals(aggregateId, envelope.getAggregateId());
        assertEquals(occurredAt, envelope.getOccurredAt());
        assertEquals("identity-service", envelope.getProducer());
        assertEquals(correlationId, envelope.getCorrelationId());
        assertEquals(Map.of("userId", "user-123"), envelope.getPayload());
    }
}
