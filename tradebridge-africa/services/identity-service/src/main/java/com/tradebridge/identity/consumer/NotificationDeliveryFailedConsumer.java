package com.tradebridge.identity.consumer;

import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.consumer.BaseConsumer;
import com.tradebridge.messaging.consumer.ConsumerRegistry;
import com.tradebridge.messaging.consumer.EventHandler;
import com.tradebridge.messaging.dlq.DeadLetterRouter;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventTypeRegistry;
import com.tradebridge.messaging.inbox.InboxGuard;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Records notification delivery failures relevant to security notifications
 * (spec §3.3 "record delivery failure for security notifications").
 *
 * <p>Consumes {@code notification.delivery.failed.v1} from
 * {@code notification.events}. Payload is a {@code Map} — we only need
 * the failing notification ID and reason.
 *
 * <p>This consumer is not actionable for the identity service beyond
 * audit — a stuck OTP delivery simply means the user must retry.
 */
@Component
public class NotificationDeliveryFailedConsumer extends BaseConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryFailedConsumer.class);
    private static final String CONSUMER_NAME = "identity.notification.delivery.failed";

    public NotificationDeliveryFailedConsumer(
            ConsumerRegistry registry,
            InboxGuard inboxGuard,
            EventTypeRegistry typeRegistry,
            DeadLetterRouter dlqRouter,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            MessagingProperties messagingProperties) {
        super(registry, inboxGuard, typeRegistry, dlqRouter, objectMapper,
                messagingProperties.getConsumer(), CONSUMER_NAME);
    }

    @KafkaListener(
            topics = "notification.events",
            groupId = "identity-service",
            containerFactory = "envelopeKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, EventEnvelope> record) {
        handleRecord(record);
    }
}
