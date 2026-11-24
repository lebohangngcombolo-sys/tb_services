package com.tradebridge.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.producer.TransactionalEventPublisher;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Wires the outbox relay and transactional publisher <b>only</b> when the
 * service declares an {@link OutboxEntityProvider} bean.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(OutboxConfig.OutboxEntityProvider.class)
public class OutboxConfig {

    @Bean
    @ConditionalOnMissingBean
    public TransactionalEventPublisher transactionalEventPublisher(
            EntityManager entityManager,
            ObjectMapper objectMapper,
            MessagingProperties properties,
            OutboxEntityProvider provider) {
        return new TransactionalEventPublisher(entityManager, objectMapper,
                properties.getOutbox(), provider.outboxEntityClass());
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRelay outboxRelay(
            MessagingProperties properties,
            KafkaTemplate<String, EventEnvelope> kafkaTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            OutboxEntityProvider provider) {
        return new OutboxRelay(provider.outboxEntityClass(),
                properties.getOutbox(), kafkaTemplate, objectMapper, transactionManager);
    }

    public interface OutboxEntityProvider {
        Class<? extends OutboxEntry> outboxEntityClass();
    }
}
