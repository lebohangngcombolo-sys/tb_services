package com.tradebridge.observability.correlation;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.context.RequestContext;
import com.tradebridge.common.context.RequestContextHolder;
import com.tradebridge.observability.logging.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that:
 * <ol>
 *   <li>Reads (or generates) a correlation ID from {@code X-Correlation-Id}.</li>
 *   <li>Populates {@link CorrelationContext} and {@link RequestContextHolder}.</li>
 *   <li>Sets it on the response header so clients can echo it.</li>
 *   <li>Exposes it to MDC for structured logging.</li>
 * </ol>
 *
 * <p>Runs as the very first filter in the servlet chain so that all
 * downstream logging has the ID available.
 *
 * <p>Reactive services (Gateway) should not use this class — they build
 * their own WebFilter (in the api-gateway module) that populates the
 * same headers and forwards to downstream services.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final int MAX_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = sanitize(request.getHeader(HEADER));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        CorrelationContext.set(correlationId);

        String clientIp = extractClientIp(request);
        RequestContextHolder.set(new RequestContext(
                correlationId, null, null, null, null, null, clientIp));

        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.CORRELATION_ID);
            RequestContextHolder.clear();
            CorrelationContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Actuator endpoints don't need correlation IDs
        return request.getRequestURI().startsWith("/actuator");
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            return null;
        }
        // Refuse to propagate obviously hostile input (control chars, path separators)
        if (!trimmed.matches("^[A-Za-z0-9._:-]+$")) {
            return null;
        }
        return trimmed;
    }

    private static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }
}
