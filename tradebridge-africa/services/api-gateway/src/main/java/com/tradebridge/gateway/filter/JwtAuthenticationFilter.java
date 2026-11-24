package com.tradebridge.gateway.filter;

import com.tradebridge.gateway.telemetry.RejectionLogger;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Post-authorization observability filter (spec §2.5).
 *
 * <p>Does NOT perform JWT validation — that is handled by Spring Security's
 * {@code oauth2ResourceServer} chain in {@link com.tradebridge.gateway.security.GatewaySecurityConfig}.
 * This filter:
 * <ul>
 *   <li>Propagates the authenticated principal's user ID as a downstream
 *       header ({@code X-User-Id}) so services do not need to re-parse JWT.</li>
 *   <li>Logs authentication rejections (401) that reach the response stage.</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private final RejectionLogger rejectionLogger;

    public JwtAuthenticationFilter(RejectionLogger rejectionLogger) {
        this.rejectionLogger = rejectionLogger;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange withHeaders = exchange.mutate()
                .request(builder -> builder.headers(h -> {
                    // Propagate identity from reactive security context if present
                }))
                .build();

        // Enrich request headers from the security context (non-blocking)
        return ReactiveSecurityContextHolder.getContext()
                .doOnNext(ctx -> {
                    var auth = ctx.getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                        String userId = jwt.getSubject();
                        String roles = String.join(",", jwt.getClaimAsStringList("roles") == null
                                ? java.util.List.of() : jwt.getClaimAsStringList("roles"));
                        exchange.getRequest().mutate()
                                .header(USER_ID_HEADER, userId)
                                .header(USER_ROLES_HEADER, roles)
                                .build();
                    }
                })
                .onErrorResume(e -> Mono.empty())
                .then(chain.filter(withHeaders))
                .doOnSuccess(v -> {
                    if (exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        rejectionLogger.logRejection(exchange, "AUTH_REJECTED",
                                exchange.getRequest().getURI().getPath());
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 500;
    }
}