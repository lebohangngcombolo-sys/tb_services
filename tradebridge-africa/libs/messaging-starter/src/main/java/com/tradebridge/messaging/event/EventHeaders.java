package com.tradebridge.messaging.event;

/**
 * Standard Kafka headers used by producers and consumers. Values are
 * plain UTF-8 strings so that any Kafka client can inspect them without
 * schema knowledge.
 */
public final class EventHeaders {

    private EventHeaders() { }

    public static final String EVENT_ID = "x-event-id";
    public static final String EVENT_TYPE = "x-event-type";
    public static final String EVENT_VERSION = "x-event-version";
    public static final String AGGREGATE_ID = "x-aggregate-id";
    public static final String PRODUCER = "x-producer";
    public static final String CORRELATION_ID = "x-correlation-id";
    public static final String CAUSATION_ID = "x-causation-id";
    public static final String OCCURRED_AT = "x-occurred-at";
}
