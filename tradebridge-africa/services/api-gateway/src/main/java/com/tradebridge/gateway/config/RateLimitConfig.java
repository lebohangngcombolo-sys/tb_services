package com.tradebridge.gateway.config;

import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.response.ErrorResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.gateway.telemetry.RejectionLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory rate limiter for sensitive routes (spec §2.5
 * "Enforce rate limits on login, OTP and password-reset routes").
 *
 * <p>Fixed-window counter keyed by {@code route-bucket + client-IP}.
 * Production deployments should replace this with a Redis-backed limiter
 * (Spring Cloud Gateway's {@code RequestRateLimiter} with {@code RedisRateLimiter})
 * for cluster-wide consistency. This implementation is intentionally
 * minimal — the spec's requirement is an enforcement surface, not a
 * particular backing store.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Path prefixes that trigger rate limiting. Order matters — first match
     * wins.
     */
    private static final Map<String, String> LIMIT_BUCKETS = Map.of(
            "/api/v1/identity/login", "login",
            "/api/v1/identity/mfa/challenge", "otp",
            "/api/v1/identity/mfa/verify", "otp",
            "/api/v1/identity/verify-email", "otp",
            "/api/v1/identity/resend-verification", "otp",
            "/api/v1/identity/password/forgot", "passwordReset",
            "/api/v1/identity/password/reset", "passwordReset",
            "/api/v1/identity/register", "registration"
    );

    @Bean
    public GlobalFilter rateLimitFilter(GatewayProperties properties,
                                         RejectionLogger rejectionLogger,
                                         ObjectMapper objectMapper) {
        return new OrderedGlobalFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                String path = exchange.getRequest().getURI().getPath();
                String bucket = matchBucket(path);
                if (bucket == null) {
                    return chain.filter(exchange);
                }

                String clientKey = resolveClientKey(exchange) + "|" + bucket;
                GatewayProperties.EndpointLimit limit = pickLimit(properties, bucket);
                if (limit == null) {
                    return chain.filter(exchange);
                }

                if (OverLimitRegistry.isOverLimit(clientKey, limit)) {
                    rejectionLogger.logRejection(exchange, "RATE_LIMIT_EXCEEDED", path);
                    return writeError(exchange, objectMapper,
                            ErrorCode.RATE_LIMIT_EXCEEDED,
                            "Too many requests; try again later");
                }
                return chain.filter(exchange);
            }

            @Override
            public int getOrder() {
                return Ordered.HIGHEST_PRECEDENCE + 100;
            }
        };
    }

    private interface OrderedGlobalFilter extends GlobalFilter, Ordered { }

    private static String matchBucket(String path) {
        for (var entry : LIMIT_BUCKETS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static GatewayProperties.EndpointLimit pickLimit(GatewayProperties props, String bucket) {
        return switch (bucket) {
            case "login" -> props.getRateLimit().getLogin();
            case "otp" -> props.getRateLimit().getOtp();
            case "passwordReset" -> props.getRateLimit().getPasswordReset();
            case "registration" -> props.getRateLimit().getRegistration();
            default -> null;
        };
    }

    private static String resolveClientKey(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote == null ? "unknown" : remote.getAddress().getHostAddress();
    }

    private static Mono<Void> writeError(ServerWebExchange exchange, ObjectMapper mapper,
                                          ErrorCode code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(GatewayHeaders.CORRELATION_ID);
        ErrorResponse body = ResponseFactory.error(code, message);
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    /** Fixed-window counter per client+bucket. */
    static final class OverLimitRegistry {
        private static final ConcurrentHashMap<String, Entry> WINDOWS = new ConcurrentHashMap<>();

        private OverLimitRegistry() { }

        static boolean isOverLimit(String key, GatewayProperties.EndpointLimit limit) {
            Instant now = Instant.now();
            long windowMs = limit.getWindow().toMillis();
            Entry current = WINDOWS.compute(key, (k, old) -> {
                if (old == null || old.windowStart.plusMillis(windowMs).isBefore(now)) {
                    return new Entry(now, new AtomicLong(1));
                }
                old.count.incrementAndGet();
                return old;
            });
            return current.count.get() > limit.getRequestsPerWindow();
        }

        private record Entry(Instant windowStart, AtomicLong count) { }
    }

    /** Names used by the gateway (header names, header prefixes). */
    static final class GatewayHeaders {
        static final String CORRELATION_ID = "X-Correlation-Id";
    }
}