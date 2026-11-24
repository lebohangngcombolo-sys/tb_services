package com.tradebridge.messaging.producer;

import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventHeaders;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Direct producer for non-transactional, non-critical events. Business
 * events must go through {@link TransactionalEventPublisher} instead.
 */
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, EventEnvelope> kafkaTemplate) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate");
    }

    @Override
    public CompletableFuture<Void> publish(String topic, EventEnvelope envelope) {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(envelope, "envelope");

        ProducerRecord<String, EventEnvelope> record = new ProducerRecord<>(
                topic, null, envelope.getAggregateId().toString(), envelope);
        addHeaders(record, envelope);

        CompletableFuture<SendResult<String, EventEnvelope>> send =
                kafkaTemplate.send(record);
        return send.thenAccept(result -> log.debug(
                        "Published {} to {} offset={}", envelope.getEventType(), topic,
                        result.getRecordMetadata().offset()))
                .exceptionally(ex -> {
                    log.error("Failed to publish {} to {}", envelope.getEventType(), topic, ex);
                    throw new IllegalStateException(ex);
                });
    }

    private static void addHeaders(ProducerRecord<String, EventEnvelope> record, EventEnvelope e) {
        record.headers().add(EventHeaders.EVENT_ID, e.getEventId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.EVENT_TYPE, e.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.EVENT_VERSION, String.valueOf(e.getEventVersion()).getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.AGGREGATE_ID, e.getAggregateId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.PRODUCER, e.getProducer().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.OCCURRED_AT, e.getOccurredAt().toString().getBytes(StandardCharsets.UTF_8));
        if (e.getCorrelationId() != null) {
            record.headers().add(EventHeaders.CORRELATION_ID, e.getCorrelationId().toString().getBytes(StandardCharsets.UTF_8));
        }
        if (e.getCausationId() != null) {
            record.headers().add(EventHeaders.CAUSATION_ID, e.getCausationId().toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
