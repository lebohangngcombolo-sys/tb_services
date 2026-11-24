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
 * Consumes {@code country.configuration.published.v1} to refresh any
 * cached requirements (spec §6.3).
 */
@Component
public class CountryConfigurationPublishedConsumer extends BaseConsumer {

    public static final String CONSUMER_NAME = "customer.country.configuration.published";

    public CountryConfigurationPublishedConsumer(ConsumerRegistry registry,
                                                   InboxGuard inboxGuard,
                                                   EventTypeRegistry typeRegistry,
                                                   DeadLetterRouter dlqRouter,
                                                   ObjectMapper objectMapper,
                                                   MessagingProperties messagingProperties) {
        super(registry, inboxGuard, typeRegistry, dlqRouter, objectMapper,
                messagingProperties.getConsumer(), CONSUMER_NAME);
    }

    @KafkaListener(
            topics = "country.events",
            groupId = "customer-service",
            containerFactory = "envelopeKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, EventEnvelope> record) {
        handleRecord(record);
    }
}