package com.tradebridge.messaging.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.event.EventEnvelope;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;
import java.util.Objects;

/**
 * Deserializes JSON bytes into {@link EventEnvelope}. Payload remains as
 * a generic {@code Map<String,Object>} / {@code JsonNode} — the consumer
 * base casts it to its concrete type via Jackson.
 */
public class JsonEventDeserializer implements Deserializer<EventEnvelope> {

    private final ObjectMapper mapper;

    public JsonEventDeserializer() {
        this(defaultMapper());
    }

    public JsonEventDeserializer(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) { /* no-op */ }

    @Override
    public EventEnvelope deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(data, EventEnvelope.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to deserialize EventEnvelope from topic '" + topic + "'", e);
        }
    }

    @Override
    public void close() { /* no-op */ }

    static ObjectMapper defaultMapper() {
        return com.tradebridge.common.util.JsonUtils.mapper().copy();
    }
}
