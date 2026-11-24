package com.tradebridge.messaging.dlq;

import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.event.EventEnvelope;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Routes a failed message to the topic's {@code .dlq} counterpart.
 *
 * <p>Headers preserve the original {@code eventId}, {@code eventType},
 * {@code aggregateId}, {@code consumerName}, and the failure reason so
 * operators can replay or inspect without re-parsing the payload.
 */
public class DeadLetterRouter {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterRouter.class);

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final MessagingProperties.Dlq props;

    public DeadLetterRouter(KafkaTemplate<String, EventEnvelope> kafkaTemplate,
                            MessagingProperties.Dlq props) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate");
        this.props = Objects.requireNonNull(props, "props");
    }

    public String dlqTopicFor(String originalTopic) {
        if (originalTopic.endsWith(props.getSuffix())) {
            return originalTopic;
        }
        return originalTopic + props.getSuffix();
    }

    public void route(String originalTopic, EventEnvelope envelope, String consumerName, String reason) {
        if (!props.isEnabled()) {
            log.warn("DLQ disabled — dropping event {} from {}", envelope.getEventId(), originalTopic);
            return;
        }
        String dlqTopic = dlqTopicFor(originalTopic);

        ProducerRecord<String, EventEnvelope> record = new ProducerRecord<>(
                dlqTopic, null, envelope.getAggregateId().toString(), envelope);

        record.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC,
                originalTopic.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaHeaders.DLT_EXCEPTION_MESSAGE,
                (reason == null ? "unknown" : reason).getBytes(StandardCharsets.UTF_8));
        record.headers().add("x-consumer-name", consumerName.getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
            log.info("Routed event {} from {} to {}", envelope.getEventId(), originalTopic, dlqTopic);
        } catch (Exception e) {
            log.error("Failed to route event {} to DLQ {}: {}",
                    envelope.getEventId(), dlqTopic, e.getMessage(), e);
        }
    }
}
