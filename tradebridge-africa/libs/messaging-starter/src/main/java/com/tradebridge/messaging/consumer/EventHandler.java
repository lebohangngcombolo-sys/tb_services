package com.tradebridge.messaging.consumer;

import com.tradebridge.messaging.event.EventEnvelope;

/**
 * Typed handler for a single event type.
 *
 * @param <T> concrete payload type
 */
public interface EventHandler<T> {

    /** The event type this handler wants — must end in '.v<n>'. */
    String eventType();

    /** The payload class to which the raw JSON is deserialized. */
    Class<T> payloadType();

    /**
     * Handle the event. Implementations should be idempotent; the base
     * consumer already wraps calls in {@link com.tradebridge.messaging.inbox.InboxGuard}
     * when possible, but business-level idempotency is still the handler's job.
     */
    void handle(EventEnvelope envelope, T payload);
}
