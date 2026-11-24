package com.tradebridge.identity.service;

import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.producer.EventPublisher;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.common.util.DateTimeUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Protected handoff for OTP delivery (spec §3.3, §11 decision 3).
 *
 * <p>The raw OTP is <b>never</b> placed on Kafka or persisted. Instead,
 * Identity persists the challenge and publishes a lightweight event
 * carrying only the challenge ID. Notification Service, when it needs
 * the actual code, calls a short-lived, authenticated internal endpoint
 * ({@code POST /api/v1/identity/internal/challenge-delivery/{challengeId}})
 * which returns the code once and consumes the challenge.
 *
 * <p>This class simply publishes the lightweight {@code *requested}
 * events. The internal callback controller lives in Part 3b.
 */
@Component
public class SecurityNotificationHandoff {

    private final EventPublisher eventPublisher;
    private final String producerServiceName;

    public SecurityNotificationHandoff(
            @Qualifier("transactionalEventPublisher") EventPublisher eventPublisher,
            @Value("${tradebridge.identity.producer-service-name:identity-service}") String producerServiceName) {
        this.eventPublisher = eventPublisher;
        this.producerServiceName = producerServiceName;
    }

    @Transactional
    public void requestEmailVerification(UUID userId, UUID challengeId, String maskedDestination, UUID correlationId) {
        publish("identity.user.email.verification.requested.v1",
                userId, correlationId,
                Map.of("userId", userId.toString(),
                       "challengeId", challengeId.toString(),
                       "destinationMasked", maskedDestination));
    }

    @Transactional
    public void requestMfaOtp(UUID userId, UUID challengeId, String maskedDestination, UUID correlationId) {
        publish("identity.user.mfa.otp.requested.v1",
                userId, correlationId,
                Map.of("userId", userId.toString(),
                       "challengeId", challengeId.toString(),
                       "destinationMasked", maskedDestination));
    }

    @Transactional
    public void requestPasswordReset(UUID userId, UUID challengeId, String maskedDestination, UUID correlationId) {
        publish("identity.user.password.reset.requested.v1",
                userId, correlationId,
                Map.of("userId", userId.toString(),
                       "challengeId", challengeId.toString(),
                       "destinationMasked", maskedDestination));
    }

    private void publish(String eventType, UUID aggregateId, UUID correlationId, Map<String, Object> payload) {
        EventEnvelope envelope = new EventEnvelope(
                UuidV7Generator.next(),
                eventType,
                parseVersion(eventType),
                aggregateId,
                Instant.now(),
                producerServiceName,
                correlationId,
                null,
                payload);
        eventPublisher.publish("identity.events", envelope);
    }

    private static int parseVersion(String eventType) {
        int dotV = eventType.lastIndexOf(".v");
        if (dotV < 0) {
            throw new IllegalArgumentException("Event type must end with '.v<n>': " + eventType);
        }
        return Integer.parseInt(eventType.substring(dotV + 2));
    }
}
