package com.tradebridge.gateway.filter;

import com.tradebridge.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive correlation-ID filter (spec §2.4 "Propagate correlation IDs").
 *
 * <p>Reads {@code X-Correlation-Id} from the incoming request or generates
 * one; injects it into the downstream request and the response header so
 * clients can correlate.
 *
 * <p>Uses the reactive {@link ServerWebExchange} — never {@code MDC}
 * (that is thread-local and unusable in a reactive stack).
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    private static final int MAX_LENGTH = 128;

    private final GatewayProperties properties;

    public CorrelationIdFilter(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.getTracing().isPropagateCorrelationId()) {
            return chain.filter(exchange);
        }
        String correlationId = sanitize(exchange.getRequest().getHeaders().getFirst(HEADER));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER, correlationId)
                .build();
        ServerWebExchange withRequest = exchange.mutate().request(request).build();
        withRequest.getResponse().getHeaders().set(HEADER, correlationId);

        // Make available for downstream error handlers via exchange attributes
        withRequest.getAttributes().put(HEADER, correlationId);

        return chain.filter(withRequest);
    }

    private static String sanitize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) return null;
        if (!trimmed.matches("^[A-Za-z0-9._:-]+$")) return null;
        return trimmed;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}