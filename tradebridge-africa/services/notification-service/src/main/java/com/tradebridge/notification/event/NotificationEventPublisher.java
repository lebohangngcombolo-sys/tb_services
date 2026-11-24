package com.tradebridge.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.notification.domain.Notification;
import com.tradebridge.notification.domain.NotificationDelivery;
import com.tradebridge.notification.event.payload.DeliveryExhaustedPayload;
import com.tradebridge.notification.event.payload.DeliveryFailedPayload;
import com.tradebridge.notification.event.payload.DeliveryRequestedPayload;
import com.tradebridge.notification.event.payload.DeliverySucceededPayload;
import com.tradebridge.notification.event.payload.InAppCreatedPayload;
import com.tradebridge.notification.event.payload.InAppReadPayload;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.producer.EventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes {@code notification.events} (spec §5.3) via the transactional
 * outbox (spec §1.4). Every method is {@code @Transactional} so the
 * outbox row is persisted atomically with the domain change.
 */
@Component
public class NotificationEventPublisher {

    public static final String TOPIC = "notification.events";
    public static final String PRODUCER = "notification-service";

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public NotificationEventPublisher(
            @Qualifier("transactionalEventPublisher") EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publishDeliveryRequested(Notification notification, String provider, UUID correlationId) {
        publish("notification.delivery.requested.v1", notification.getId(),
                new DeliveryRequestedPayload(
                        notification.getId(), notification.getChannel().name(),
                        notification.getTemplateCode(), provider),
                correlationId);
    }

    @Transactional
    public void publishDeliverySucceeded(Notification notification,
                                          NotificationDelivery delivery, UUID correlationId) {
        publish("notification.delivery.succeeded.v1", notification.getId(),
                new DeliverySucceededPayload(
                        notification.getId(), delivery.getId(), delivery.getProvider(),
                        delivery.getProviderMessageId()),
                correlationId);
    }

    @Transactional
    public void publishDeliveryFailed(Notification notification,
                                        NotificationDelivery delivery, UUID correlationId) {
        publish("notification.delivery.failed.v1", notification.getId(),
                new DeliveryFailedPayload(
                        notification.getId(), delivery.getId(), delivery.getProvider(),
                        delivery.getFailureCode(), delivery.getFailureReason(),
                        notification.getRecipientUserId()),
                correlationId);
    }

    @Transactional
    public void publishInAppCreated(Notification notification, UUID correlationId) {
        publish("notification.inapp.created.v1", notification.getId(),
                new InAppCreatedPayload(
                        notification.getId(), notification.getRecipientUserId(),
                        notification.getSubject()),
                correlationId);
    }

    @Transactional
    public void publishInAppRead(Notification notification, UUID correlationId) {
        publish("notification.inapp.read.v1", notification.getId(),
                new InAppReadPayload(notification.getId(), notification.getRecipientUserId()),
                correlationId);
    }

    @Transactional
    public void publishDeliveryExhausted(Notification notification, int attempts, UUID correlationId) {
        publish("notification.delivery.exhausted.v1", notification.getId(),
                new DeliveryExhaustedPayload(notification.getId(), attempts),
                correlationId);
    }

    // ------------------------------------------------------------------ internal

    private void publish(String eventType, UUID aggregateId, Object payload, UUID correlationId) {
        int version = parseVersion(eventType);
        Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);
        EventEnvelope envelope = new EventEnvelope(
                UuidV7Generator.next(), eventType, version,
                aggregateId, Instant.now(), PRODUCER, correlationId, null, payloadMap);
        eventPublisher.publish(TOPIC, envelope);
    }

    private static int parseVersion(String eventType) {
        int dotV = eventType.lastIndexOf(".v");
        if (dotV < 0) throw new IllegalArgumentException("Event type must end with '.v<n>': " + eventType);
        return Integer.parseInt(eventType.substring(dotV + 2));
    }
}