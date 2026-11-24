package com.tradebridge.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.consumer.BaseConsumer;
import com.tradebridge.messaging.consumer.ConsumerRegistry;
import com.tradebridge.messaging.dlq.DeadLetterRouter;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventTypeRegistry;
import com.tradebridge.messaging.inbox.InboxGuard;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code identity.user.password.changed.v1}. Sends a security
 * alert email if the event payload includes an email (current payload
 * does not — this consumer logs and awaits the future extended payload).
 */
@Component
public class PasswordChangedConsumer extends BaseConsumer {

    public static final String CONSUMER_NAME = "notification.identity.password.changed";

    public PasswordChangedConsumer(ConsumerRegistry registry,
                                    InboxGuard inboxGuard,
                                    EventTypeRegistry typeRegistry,
                                    DeadLetterRouter dlqRouter,
                                    ObjectMapper objectMapper,
                                    MessagingProperties messagingProperties) {
        super(registry, inboxGuard, typeRegistry, dlqRouter, objectMapper,
                messagingProperties.getConsumer(), CONSUMER_NAME);
    }

    @KafkaListener(
            topics = "identity.events",
            groupId = "notification-service",
            containerFactory = "envelopeKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, EventEnvelope> record) {
        handleRecord(record);
    }
}