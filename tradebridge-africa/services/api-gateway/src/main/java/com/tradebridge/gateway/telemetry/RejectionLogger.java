package com.tradebridge.gateway.telemetry;

import com.tradebridge.observability.logging.SensitiveDataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

/**
 * Structured logger for rejected requests (spec §2.5 "Do not log passwords,
 * tokens, OTPs or sensitive KYC data").
 *
 * <p>Logs only: correlation ID, path, method, reason, client IP (masked).
 * Never logs headers, query strings, or bodies.
 */
@Component
public class RejectionLogger {

    private static final Logger log = LoggerFactory.getLogger("GATEWAY_REJECTION");

    public void logRejection(ServerWebExchange exchange, String reason, String path) {
        String correlationId = (String) exchange.getAttributes()
                .get("X-Correlation-Id");
        String method = exchange.getRequest().getMethod().name();
        String clientIp = resolveClientIp(exchange);
        log.warn("reason={} method={} path={} clientIp={} correlationId={} occurredAt={}",
                reason,
                method,
                SensitiveDataMasker.maskMessage(path),
                clientIp,
                correlationId == null ? "-" : correlationId,
                Instant.now());
    }

    private static String resolveClientIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        var addr = exchange.getRequest().getRemoteAddress();
        return addr == null ? "unknown" : addr.getAddress().getHostAddress();
    }
}