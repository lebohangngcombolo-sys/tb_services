package com.tradebridge.observability.metrics;

/**
 * Canonical metric names. Prefix all names with {@code tb.} to avoid
 * collision with Spring Boot's defaults and Micrometer's conventions.
 *
 * <p>Label rules per spec §7.4: metrics must not carry personal
 * identifiers (email, phone, user ID) as labels.
 */
public final class MetricNames {

    private MetricNames() { }

    // --------------------------------- HTTP
    public static final String HTTP_SERVER_REQUESTS = "tb.http.server.requests";
    public static final String HTTP_CLIENT_REQUESTS = "tb.http.client.requests";
    public static final String HTTP_SERVER_ERRORS = "tb.http.server.errors";

    // --------------------------------- Kafka
    public static final String KAFKA_EVENTS_PUBLISHED = "tb.kafka.events.published";
    public static final String KAFKA_EVENTS_CONSUMED = "tb.kafka.events.consumed";
    public static final String KAFKA_EVENTS_DLQ = "tb.kafka.events.dlq";
    public static final String KAFKA_OUTBOX_PENDING = "tb.kafka.outbox.pending";
    public static final String KAFKA_OUTBOX_FAILED = "tb.kafka.outbox.failed";

    // --------------------------------- Auth
    public static final String AUTH_LOGIN_SUCCESS = "tb.auth.login.success";
    public static final String AUTH_LOGIN_FAILURE = "tb.auth.login.failure";
    public static final String AUTH_MFA_CHALLENGES = "tb.auth.mfa.challenges";

    // --------------------------------- KYC
    public static final String KYC_CASES_SUBMITTED = "tb.kyc.cases.submitted";
    public static final String KYC_DECISIONS = "tb.kyc.decisions";

    // --------------------------------- Common tag keys
    public static final String TAG_SERVICE = "service";
    public static final String TAG_TOPIC = "topic";
    public static final String TAG_EVENT_TYPE = "event_type";
    public static final String TAG_CONSUMER = "consumer";
    public static final String TAG_METHOD = "method";
    public static final String TAG_URI_TEMPLATE = "uri";
    public static final String TAG_STATUS = "status";
    public static final String TAG_OUTCOME = "outcome";
}
