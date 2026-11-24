package com.tradebridge.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.consumer.ConsumerRegistry;
import com.tradebridge.messaging.consumer.EventHandler;
import com.tradebridge.messaging.dlq.DeadLetterRouter;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventTypeRegistry;
import com.tradebridge.messaging.inbox.InboxConfig;
import com.tradebridge.messaging.inbox.InboxGuard;
import com.tradebridge.messaging.outbox.OutboxConfig;
import com.tradebridge.messaging.producer.KafkaEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * Root autoconfiguration for the messaging starter.
 *
 * <p>Wires:
 * <ul>
 *   <li>{@link EventTypeRegistry}</li>
 *   <li>{@link ConsumerRegistry} (populated from {@link EventHandler} beans)</li>
 *   <li>{@link KafkaEventPublisher}</li>
 *   <li>{@link DeadLetterRouter}</li>
 *   <li>{@link OutboxConfig} (only when a service provides an {@code OutboxEntityProvider})</li>
 *   <li>{@link InboxConfig} (only when a service provides an {@code InboxEntityProvider})</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@EnableConfigurationProperties(MessagingProperties.class)
@Import({KafkaProducerConfig.class, KafkaConsumerConfig.class})
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventTypeRegistry eventTypeRegistry() {
        return new EventTypeRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerRegistry consumerRegistry(ObjectProvider<EventHandler<?>> handlers) {
        ConsumerRegistry registry = new ConsumerRegistry();
        handlers.orderedStream().forEach(registry::register);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(KafkaTemplate.class)
    public KafkaEventPublisher kafkaEventPublisher(
            KafkaTemplate<String, EventEnvelope> envelopeKafkaTemplate) {
        return new KafkaEventPublisher(envelopeKafkaTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tradebridge.messaging.dlq", name = "enabled",
            matchIfMissing = true)
    public DeadLetterRouter deadLetterRouter(
            KafkaTemplate<String, EventEnvelope> envelopeKafkaTemplate,
            MessagingProperties properties) {
        return new DeadLetterRouter(envelopeKafkaTemplate, properties.getDlq());
    }

    /** Outbox wired only when a service provides the entity class. */
    @AutoConfiguration
    @ConditionalOnClass(name = "jakarta.persistence.EntityManager")
    @ConditionalOnProperty(prefix = "tradebridge.messaging.outbox", name = "enabled",
            matchIfMissing = true)
    @Import(OutboxConfig.class)
    public static class OutboxAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Map<String, Object> outboxMarker() {
            return Map.of("outbox", "enabled");
        }
    }

    /** Inbox wired only when a service provides the entity class. */
    @AutoConfiguration
    @ConditionalOnClass(name = "jakarta.persistence.EntityManager")
    @ConditionalOnProperty(prefix = "tradebridge.messaging.inbox", name = "enabled",
            matchIfMissing = true)
    @Import(InboxConfig.class)
    public static class InboxAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Map<String, Object> inboxMarker() {
            return Map.of("inbox", "enabled");
        }
    }
}
