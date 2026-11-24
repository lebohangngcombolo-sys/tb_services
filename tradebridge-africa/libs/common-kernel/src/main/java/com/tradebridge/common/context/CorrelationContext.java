package com.tradebridge.common.context;

import java.util.UUID;

/**
 * Thread-scoped holder for the current correlation ID.
 *
 * <p>Filters in {@code observability-starter} and the API Gateway populate
 * this. It is the source-of-truth for the {@code correlationId} field of
 * every response envelope.
 *
 * <p>NOTE: this uses a plain {@link ThreadLocal}. Reactive stacks (the Gateway)
 * must not use this; they should call {@code ResponseFactory} with an explicit
 * correlationId instead.
 */
public final class CorrelationContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationContext() { }

    public static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public static String current() {
        return CURRENT.get();
    }

    public static String currentOrGenerate() {
        String existing = CURRENT.get();
        if (existing == null || existing.isBlank()) {
            String generated = UUID.randomUUID().toString();
            CURRENT.set(generated);
            return generated;
        }
        return existing;
    }

    public static void clear() {
        CURRENT.remove();
    }
}