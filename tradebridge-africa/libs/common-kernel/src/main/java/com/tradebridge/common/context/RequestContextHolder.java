
package com.tradebridge.common.context;

import java.util.Optional;
import java.util.UUID;

/**
 * ThreadLocal holder for the current request context.
 */
public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> HOLDER =
        new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext context) {
        if (context == null) {
            HOLDER.remove();
            return;
        }

        HOLDER.set(context);
        CorrelationContext.set(context.correlationId());
    }

    public static RequestContext current() {
        return HOLDER.get();
    }

    public static Optional<RequestContext> currentOptional() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static Optional<String> currentCorrelationId() {
        RequestContext context = HOLDER.get();

        return context == null
            ? Optional.empty()
            : Optional.ofNullable(context.correlationId());
    }

    public static Optional<UUID> currentUserId() {
        RequestContext context = HOLDER.get();

        return context == null
            ? Optional.empty()
            : context.userIdOptional();
    }

    public static void clear() {
        HOLDER.remove();
        CorrelationContext.clear();
    }
}
