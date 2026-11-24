package com.tradebridge.identity.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.identity.config.EventHandlerContributor;
import com.tradebridge.identity.config.EventTypeContributor;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.consumer.ConsumerRegistry;
import com.tradebridge.messaging.consumer.EventHandler;
import com.tradebridge.messaging.dlq.DeadLetterRouter;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventTypeRegistry;
import com.tradebridge.messaging.inbox.InboxGuard;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CountryConfigurationUpdatedConsumerTest {

    @Mock private InboxGuard inboxGuard;
    @Mock private DeadLetterRouter dlqRouter;

    private ConsumerRegistry registry;
    private EventTypeRegistry typeRegistry;
    private CountryConfigurationUpdatedConsumer consumer;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Register the handler from EventHandlerContributor
        EventHandlerContributor contributor = new EventHandlerContributor();
        EventHandler<Map<String, Object>> handler = contributor.countryConfigurationUpdatedHandler();

        registry = new ConsumerRegistry();
        registry.register(handler);

        typeRegistry = new EventTypeRegistry();
        EventTypeContributor etc = new EventTypeContributor(typeRegistry);
        etc.register();

        // InboxGuard always lets things through (we're unit-testing routing)
        when(inboxGuard.executeOnce(any(), anyString(), any())).thenAnswer(inv -> {
            Runnable r = inv.getArgument(2);
            r.run();
            return true;
        });

        MessagingProperties messagingProperties = new MessagingProperties();
        consumer = new CountryConfigurationUpdatedConsumer(
                registry, inboxGuard, typeRegistry, dlqRouter,
                new ObjectMapper(), messagingProperties);
    }

    @Test
    void dispatchesKnownEventToRegisteredHandler() {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                "country.configuration.updated.v1",
                1,
                UUID.randomUUID(),
                Instant.now(),
                "country-service",
                null,
                null,
                Map.of("countryCode", "ZA", "version", 3));

        ConsumerRecord<String, EventEnvelope> record =
                new ConsumerRecord<>("country.events", 0, 0L, "key", envelope);

        consumer.handleRecord(record);

        verify(inboxGuard).executeOnce(any(), anyString(), any());
        verify(dlqRouter, never()).route(any(), any(), any(), any());
    }

    @Test
    void routesUnknownEventTypeToDlq() {
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID(),
                "country.this.does.not.exist.v1",
                1,
                UUID.randomUUID(),
                Instant.now(),
                "country-service",
                null,
                null,
                Map.of());

        ConsumerRecord<String, EventEnvelope> record =
                new ConsumerRecord<>("country.events", 0, 0L, "key", envelope);

        consumer.handleRecord(record);

        verify(dlqRouter).route(any(), any(), anyString(), anyString());
        verify(inboxGuard, never()).executeOnce(any(), anyString(), any());
    }
}
