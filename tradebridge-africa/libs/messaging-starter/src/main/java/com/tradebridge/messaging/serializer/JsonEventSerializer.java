package com.tradebridge.messaging.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.event.EventEnvelope;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;
import java.util.Objects;

/**
 * Serializes {@link EventEnvelope} to UTF-8 JSON bytes. Header-agnostic —
 * the producer layer is responsible for adding the standard headers.
 */
public class JsonEventSerializer implements Serializer<EventEnvelope> {

    private final ObjectMapper mapper;

    public JsonEventSerializer() {
        this(defaultMapper());
    }

    public JsonEventSerializer(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) { /* no-op */ }

    @Override
    public byte[] serialize(String topic, EventEnvelope data) {
        if (data == null) {
            return null;
        }
        try {
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize EventEnvelope", e);
        }
    }

    @Override
    public void close() { /* no-op */ }

    static ObjectMapper defaultMapper() {
        return com.tradebridge.common.util.JsonUtils.mapper().copy();
    }
}
