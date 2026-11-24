package com.tradebridge.messaging.config;

import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.serializer.JsonEventSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer factory and {@link KafkaTemplate} for {@link EventEnvelope} values.
 *
 * <p>Reads common settings from Spring's {@link KafkaProperties} so the
 * `spring.kafka.bootstrap-servers` etc. configured by the service still apply.
 * Adds the tradebridge-specific defaults: acks=all, idempotence, retries.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "tradebridge.messaging.producer", name = "enabled",
        matchIfMissing = true)
public class KafkaProducerConfig {

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, EventEnvelope> envelopeProducerFactory(
            KafkaProperties kafkaProperties, MessagingProperties messagingProperties) {

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        MessagingProperties.Producer p = messagingProperties.getProducer();

        props.put(ProducerConfig.ACKS_CONFIG, p.getAcks());
        props.put(ProducerConfig.RETRIES_CONFIG, p.getRetries());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, p.isEnableIdempotence());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonEventSerializer.class);
        // Safety: idempotence requires max.in.flight <= 5
        if (p.isEnableIdempotence()) {
            props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        }

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, EventEnvelope> envelopeKafkaTemplate(
            ProducerFactory<String, EventEnvelope> envelopeProducerFactory) {
        return new KafkaTemplate<>(envelopeProducerFactory);
    }
}
