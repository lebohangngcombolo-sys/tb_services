
package com.tradebridge.messaging.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime registry of EventHandlers keyed by event type.
 *
 * Populated from Spring beans at startup and used by BaseConsumer
 * to route an incoming envelope to the correct typed handler.
 */
public class ConsumerRegistry {

    private final Map<String, EventHandler<?>> handlers = new HashMap<>();

    public ConsumerRegistry() {
    }

    public ConsumerRegistry(Map<String, EventHandler<?>> initial) {
        if (initial != null) {
            initial.values().forEach(this::register);
        }
    }

    public void register(EventHandler<?> handler) {

        Objects.requireNonNull(handler, "handler");

        String type = handler.eventType();

        if (type == null ||
            !type.matches("^[a-z][a-z0-9]*(\\.[a-z0-9]+)+\\.v\\d+$")) {

            throw new IllegalArgumentException("Invalid eventType: " + type);
        }

        EventHandler<?> previous = handlers.putIfAbsent(type, handler);

        if (previous != null && previous.getClass() != handler.getClass()) {
            throw new IllegalStateException(
                "Duplicate handler for event type " + type + ": "
                    + previous.getClass().getName()
                    + " vs "
                    + handler.getClass().getName()
            );
        }
    }

    public Optional<EventHandler<?>> find(String eventType) {
        return Optional.ofNullable(handlers.get(eventType));
    }

    public Map<String, EventHandler<?>> all() {
        return Map.copyOf(handlers);
    }
}

