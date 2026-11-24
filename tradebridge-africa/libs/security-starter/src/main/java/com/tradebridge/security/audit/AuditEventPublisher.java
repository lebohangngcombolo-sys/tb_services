package com.tradebridge.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Default security-audit sink. Writes structured audit events to SLF4J
 * under the {@code SECURITY_AUDIT} logger so downstream aggregators
 * (Loki/ELK) can pick them up without the library itself depending on Kafka.
 *
 * <p>Do NOT log sensitive material here (§2.5, §3.4). The default
 * implementation is safe for that reason — it only records identifiers,
 * action names and outcome codes.
 *
 * <p>Services that need Kafka-based auditing can override this bean and
 * publish to the identity audit topic instead.
 */
public class AuditEventPublisher {

    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void publish(SecurityAuditHook.SecurityAuditEvent event) {
        if (event == null) {
            return;
        }
        AUDIT.info(
                "action={} outcome={} actorId={} targetId={} correlationId={} occurredAt={} metadata={}",
                event.action(),
                event.outcome(),
                event.actorId(),
                event.targetId(),
                event.correlationId(),
                event.occurredAt(),
                sanitize(event.metadata()));
    }

    private Map<String, Object> sanitize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata.entrySet().stream()
                .filter(e -> !isSensitiveKey(e.getKey()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return lower.contains("password")
                || lower.contains("token")
                || lower.contains("otp")
                || lower.contains("secret")
                || lower.contains("kyc")
                || lower.contains("document");
    }

    /** Package-private for tests. */
    static Instant nowForTesting() {
        return Instant.now();
    }
}
