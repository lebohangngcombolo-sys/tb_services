package com.tradebridge.messaging.config;

import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.serializer.JsonEventDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer factory and listener container factory for {@link EventEnvelope} values.
 *
 * <p>Offsets are committed after each listener invocation completes
 * successfully. The base consumer returns only after inbox processing finishes.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "tradebridge.messaging.consumer", name = "enabled",
        matchIfMissing = true)
public class KafkaConsumerConfig {

    @Bean
    @ConditionalOnMissingBean(name = "envelopeConsumerFactory")
    public ConsumerFactory<String, EventEnvelope> envelopeConsumerFactory(
            KafkaProperties kafkaProperties, MessagingProperties messagingProperties) {

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        MessagingProperties.Consumer c = messagingProperties.getConsumer();

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, c.getAutoOffsetReset());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, c.getMaxPollRecords());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonEventDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    @ConditionalOnMissingBean(name = "envelopeKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> envelopeKafkaListenerContainerFactory(
            ConsumerFactory<String, EventEnvelope> envelopeConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(envelopeConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
