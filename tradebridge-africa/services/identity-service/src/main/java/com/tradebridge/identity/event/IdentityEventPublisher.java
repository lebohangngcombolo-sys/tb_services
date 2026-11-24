package com.tradebridge.identity.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.identity.event.payload.EmailVerificationRequestedPayload;
import com.tradebridge.identity.event.payload.MfaDisabledPayload;
import com.tradebridge.identity.event.payload.MfaEnabledPayload;
import com.tradebridge.identity.event.payload.MfaOtpRequestedPayload;
import com.tradebridge.identity.event.payload.PasswordChangedPayload;
import com.tradebridge.identity.event.payload.PasswordResetRequestedPayload;
import com.tradebridge.identity.event.payload.RoleChangedPayload;
import com.tradebridge.identity.event.payload.SessionRevokedPayload;
import com.tradebridge.identity.event.payload.StatusChangedPayload;
import com.tradebridge.identity.event.payload.UserActivatedPayload;
import com.tradebridge.identity.event.payload.UserRegisteredPayload;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.producer.EventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes identity domain events (spec §3.3) via the outbox.
 *
 * <p>Every method is {@code @Transactional(MANDATORY)} through
 * {@link EventPublisher#publish(String, EventEnvelope)} — the publisher
 * itself requires an active transaction so that the outbox row is written
 * atomically with the domain change (spec §1.4).
 */
@Component
public class IdentityEventPublisher {

    public static final String TOPIC = "identity.events";
    public static final String PRODUCER = "identity-service";

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public IdentityEventPublisher(
            @Qualifier("transactionalEventPublisher") EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ events

    @Transactional
    public void publishUserRegistered(UUID userId, String email, String countryCode, UUID correlationId) {
        publish("identity.user.registered.v1", 1, userId,
                new UserRegisteredPayload(userId.toString(), email, countryCode), correlationId);
    }

    @Transactional
    public void publishEmailVerificationRequested(UUID userId, UUID challengeId,
                                                   String destinationMasked, UUID correlationId) {
        publish("identity.user.email.verification.requested.v1", 1, userId,
                new EmailVerificationRequestedPayload(
                        userId.toString(), challengeId.toString(), destinationMasked),
                correlationId);
    }

    @Transactional
    public void publishUserActivated(UUID userId, UUID correlationId) {
        publish("identity.user.activated.v1", 1, userId,
                new UserActivatedPayload(userId.toString(), Instant.now()), correlationId);
    }

    @Transactional
    public void publishPasswordResetRequested(UUID userId, UUID challengeId,
                                               String destinationMasked, UUID correlationId) {
        publish("identity.user.password.reset.requested.v1", 1, userId,
                new PasswordResetRequestedPayload(
                        userId.toString(), challengeId.toString(), destinationMasked),
                correlationId);
    }

    @Transactional
    public void publishPasswordChanged(UUID userId, UUID correlationId) {
        publish("identity.user.password.changed.v1", 1, userId,
                new PasswordChangedPayload(userId.toString(), Instant.now()), correlationId);
    }

    @Transactional
    public void publishMfaOtpRequested(UUID userId, UUID challengeId,
                                       String destinationMasked, UUID correlationId) {
        publish("identity.user.mfa.otp.requested.v1", 1, userId,
                new MfaOtpRequestedPayload(
                        userId.toString(), challengeId.toString(), destinationMasked),
                correlationId);
    }

    @Transactional
    public void publishMfaEnabled(UUID userId, String factorType, UUID correlationId) {
        publish("identity.user.mfa.enabled.v1", 1, userId,
                new MfaEnabledPayload(userId.toString(), factorType), correlationId);
    }

    @Transactional
    public void publishMfaDisabled(UUID userId, String factorType, UUID correlationId) {
        publish("identity.user.mfa.disabled.v1", 1, userId,
                new MfaDisabledPayload(userId.toString(), factorType), correlationId);
    }

    @Transactional
    public void publishRoleChanged(UUID userId, List<String> roleCodes, UUID correlationId) {
        publish("identity.user.role.changed.v1", 1, userId,
                new RoleChangedPayload(userId.toString(), roleCodes), correlationId);
    }

    @Transactional
    public void publishStatusChanged(UUID userId, String oldStatus,
                                     String newStatus, UUID correlationId) {
        publish("identity.user.status.changed.v1", 1, userId,
                new StatusChangedPayload(userId.toString(), oldStatus, newStatus), correlationId);
    }

    @Transactional
    public void publishSessionRevoked(UUID userId, UUID sessionId, UUID correlationId) {
        publish("identity.user.session.revoked.v1", 1, userId,
                new SessionRevokedPayload(userId.toString(),
                        sessionId == null ? null : sessionId.toString()), correlationId);
    }

    // ------------------------------------------------------------------ internal

    private void publish(String eventType, int version, UUID aggregateId,
                         Object payload, UUID correlationId) {
        Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);
        EventEnvelope envelope = new EventEnvelope(
                UuidV7Generator.next(),
                eventType,
                version,
                aggregateId,
                Instant.now(),
                PRODUCER,
                correlationId,
                null,
                payloadMap);
        eventPublisher.publish(TOPIC, envelope);
    }
}
