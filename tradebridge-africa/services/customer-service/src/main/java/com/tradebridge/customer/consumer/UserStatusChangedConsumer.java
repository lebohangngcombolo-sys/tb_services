package com.tradebridge.customer.consumer;

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
 * Consumes {@code identity.user.status.changed.v1} to apply access
 * restrictions locally (spec §6.3).
 */
@Component
public class UserStatusChangedConsumer extends BaseConsumer {

    public static final String CONSUMER_NAME = "customer.identity.user.status.changed";

    public UserStatusChangedConsumer(ConsumerRegistry registry,
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
            groupId = "customer-service",
            containerFactory = "envelopeKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, EventEnvelope> record) {
        handleRecord(record);
    }
}