package com.tradebridge.observability.correlation;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.observability.logging.MdcKeys;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility for propagating the current correlation ID to
 * outgoing HTTP requests and Kafka records, and for restoring it
 * on incoming message consumption.
 *
 * <p>Servers call {@link #restoreForMessage(UUID)} inside a Kafka listener
 * before doing work, and {@link #clearAfterMessage()} in a {@code finally}
 * block. HTTP clients read {@link #currentOrGenerated()} to fill the
 * {@code X-Correlation-Id} header.
 */
public final class CorrelationIdPropagation {

    public static final String HEADER = "X-Correlation-Id";

    private CorrelationIdPropagation() { }

    public static String currentOrGenerated() {
        String current = CorrelationContext.current();
        return current != null && !current.isBlank() ? current : UUID.randomUUID().toString();
    }

    public static UUID currentOrGeneratedUuid() {
        return UUID.fromString(currentOrGenerated());
    }

    /**
     * Restore correlation state for a Kafka message. Idempotent.
     */
    public static void restoreForMessage(UUID correlationId) {
        String id = correlationId == null ? UUID.randomUUID().toString() : correlationId.toString();
        CorrelationContext.set(id);
        MDC.put(MdcKeys.CORRELATION_ID, id);
    }

    public static void clearAfterMessage() {
        MDC.remove(MdcKeys.CORRELATION_ID);
        CorrelationContext.clear();
    }
}
