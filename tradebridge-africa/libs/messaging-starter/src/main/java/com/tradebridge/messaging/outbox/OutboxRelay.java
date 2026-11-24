package com.tradebridge.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventHeaders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled poller that drains {@code PENDING} outbox rows to Kafka.
 *
 * <p>Runs every {@code tradebridge.messaging.outbox.poll-interval}. Locks
 * a batch of rows with {@code PESSIMISTIC_WRITE} so that multiple instances
 * of the service do not double-publish.
 *
 * <p>On success, marks rows {@code PUBLISHED}. On failure, increments the
 * attempt counter; after {@code max-attempts}, marks the row {@code FAILED}.
 * Failed rows are left for an operator to retry (replay procedure per §9).
 */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final Class<? extends OutboxEntry> outboxEntityClass;
    private final MessagingProperties.Outbox props;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public OutboxRelay(Class<? extends OutboxEntry> outboxEntityClass,
                       MessagingProperties.Outbox props,
                       KafkaTemplate<String, EventEnvelope> kafkaTemplate,
                       ObjectMapper objectMapper,
                       PlatformTransactionManager transactionManager) {
        this.outboxEntityClass = Objects.requireNonNull(outboxEntityClass, "outboxEntityClass");
        this.props = Objects.requireNonNull(props, "props");
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
    }

    @Scheduled(fixedDelayString = "${tradebridge.messaging.outbox.poll-interval-ms:5000}")
    public void scheduledRelay() {
        if (!props.isEnabled()) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> relayBatch());
        } catch (Exception e) {
            log.warn("Outbox relay batch failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void relayBatch() {
        String entityName = outboxEntityClass.getSimpleName();

        List<OutboxEntry> batch = entityManager
                .createQuery("SELECT o FROM " + entityName + " o WHERE o.status = :s "
                        + "ORDER BY o.createdAt ASC", OutboxEntry.class)
                .setParameter("s", OutboxStatus.PENDING)
                .setMaxResults(props.getBatchSize())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        if (batch.isEmpty()) {
            return;
        }
        log.debug("Outbox relay picked {} row(s)", batch.size());

        for (OutboxEntry row : batch) {
            try {
                publishToKafka(row);
                row.markPublished(Instant.now());
            } catch (Exception ex) {
                row.incrementAttempts();
                row.markFailed(ex.getMessage());
                log.warn("Outbox publish failed for eventId={} attempt={}/{} : {}",
                        row.getEventId(), row.getAttempts(), props.getMaxAttempts(), ex.getMessage());
                if (row.getAttempts() >= props.getMaxAttempts()) {
                    log.error("Outbox row exhausted after {} attempts, eventId={}",
                            row.getAttempts(), row.getEventId());
                } else {
                    // Revert FAILED back to PENDING so the next tick retries
                    row.markPending(row.getAggregateId(), row.getEventId(),
                            row.getEventType(), row.getTopic(), row.getPayload());
                }
            }
        }
    }

    private void publishToKafka(OutboxEntry row) throws Exception {
        EventEnvelope envelope = objectMapper.readValue(row.getPayload(), EventEnvelope.class);

        ProducerRecord<String, EventEnvelope> record = new ProducerRecord<>(
                row.getTopic(), null, envelope.getAggregateId().toString(), envelope);
        record.headers().add(EventHeaders.EVENT_ID,
                envelope.getEventId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.EVENT_TYPE,
                envelope.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.AGGREGATE_ID,
                envelope.getAggregateId().toString().getBytes(StandardCharsets.UTF_8));

        // Block briefly so we know whether this attempt succeeded
        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
    }
}
