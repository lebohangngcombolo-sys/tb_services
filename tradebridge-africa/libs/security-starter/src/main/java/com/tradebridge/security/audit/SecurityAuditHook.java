package com.tradebridge.security.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Small facade for services that want to record security events
 * (role changes, login failures, MFA changes, KYC decisions, ...).
 *
 * <p>The hook is a thin wrapper around {@link AuditEventPublisher} plus
 * a couple of convenience methods. Services can inject the hook anywhere
 * and call {@code record(...)} without coupling to the publishing mechanism.
 */
public class SecurityAuditHook {

    private final AuditEventPublisher publisher;

    public SecurityAuditHook(AuditEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    public void record(String action,
                       String outcome,
                       UUID actorId,
                       UUID targetId,
                       String correlationId,
                       Map<String, Object> metadata) {
        SecurityAuditEvent event = new SecurityAuditEvent(
                action, outcome, actorId, targetId, correlationId, Instant.now(), metadata);
        publisher.publish(event);
    }

    /** Convenience for successful actions. */
    public void success(String action, UUID actorId, UUID targetId, String correlationId) {
        record(action, "SUCCESS", actorId, targetId, correlationId, Map.of());
    }

    /** Convenience for failed actions with a reason. */
    public void failure(String action, UUID actorId, UUID targetId, String correlationId, String reasonCode) {
        record(action, "FAILURE", actorId, targetId, correlationId, Map.of("reason", reasonCode));
    }

    /** Structured event payload. Never include sensitive values in metadata. */
    public record SecurityAuditEvent(
            String action,
            String outcome,
            UUID actorId,
            UUID targetId,
            String correlationId,
            Instant occurredAt,
            Map<String, Object> metadata
    ) {
        public SecurityAuditEvent {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(outcome, "outcome");
            Objects.requireNonNull(occurredAt, "occurredAt");
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
