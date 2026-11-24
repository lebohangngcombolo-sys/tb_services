
package com.tradebridge.identity.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.consumer.BaseConsumer;
import com.tradebridge.messaging.consumer.ConsumerRegistry;
import com.tradebridge.messaging.dlq.DeadLetterRouter;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventTypeRegistry;
import com.tradebridge.messaging.inbox.InboxGuard;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Refreshes country-related identity configuration when the country
 * service publishes new configuration.
 *
 * Identity does not persist onboarding rules. It refreshes cached
 * country configuration when required.
 *
 * Currently, this consumer logs configuration version updates.
 */
@Component
public class CountryConfigurationUpdatedConsumer extends BaseConsumer {

    private static final Logger log =
        LoggerFactory.getLogger(CountryConfigurationUpdatedConsumer.class);

    private static final String CONSUMER_NAME =
        "identity.country.configuration.updated";

    public CountryConfigurationUpdatedConsumer(
        ConsumerRegistry registry,
        InboxGuard inboxGuard,
        EventTypeRegistry typeRegistry,
        DeadLetterRouter dlqRouter,
        ObjectMapper objectMapper,
        MessagingProperties messagingProperties) {

        super(
            registry,
            inboxGuard,
            typeRegistry,
            dlqRouter,
            objectMapper,
            messagingProperties.getConsumer(),
            CONSUMER_NAME
        );
    }

    @KafkaListener(
        topics = "country.events",
        groupId = "identity-service",
        containerFactory = "envelopeKafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, EventEnvelope> record) {
        handleRecord(record);
    }
}
