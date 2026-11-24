package com.tradebridge.observability.logging;

/**
 * Canonical MDC keys. Services must use these constants, not string literals,
 * so that log aggregators and dashboards can rely on consistent field names.
 */
public final class MdcKeys {

    private MdcKeys() { }

    /** Correlation ID propagated across HTTP and Kafka. */
    public static final String CORRELATION_ID = "correlationId";

    /** W3C trace ID from OpenTelemetry. */
    public static final String TRACE_ID = "traceId";

    /** W3C span ID from OpenTelemetry. */
    public static final String SPAN_ID = "spanId";

    /** Kafka producer's event ID for message-scoped logging. */
    public static final String EVENT_ID = "eventId";

    /** Kafka consumer name for message-scoped logging. */
    public static final String CONSUMER_NAME = "consumerName";

    /** Authenticated user ID (never email/phone). */
    public static final String USER_ID = "userId";

    /** Service name that produced the log line. */
    public static final String SERVICE = "service";
}
