package com.tradebridge.messaging.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.outbox.OutboxEntry;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Outbox-based publisher. Writes an outbox row <b>within the caller's current
 * transaction</b> so that the domain change and the event are committed atomically.
 * The actual Kafka publish is performed by {@link OutboxRelay}.
 *
 * <p>Spec §1.4: "The producer must use a transactional outbox so database
 * commits and event publication cannot silently diverge."
 *
 * <p>Services provide the concrete {@link OutboxEntry} subclass (via
 * {@code OutboxConfig#outboxEntityClass()}), and this publisher instantiates
 * it reflectively.
 */
public class TransactionalEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionalEventPublisher.class);

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final MessagingProperties.Outbox outboxProps;
    private final Class<? extends OutboxEntry> outboxEntityClass;

    public TransactionalEventPublisher(EntityManager entityManager,
                                       ObjectMapper objectMapper,
                                       MessagingProperties.Outbox outboxProps,
                                       Class<? extends OutboxEntry> outboxEntityClass) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.outboxProps = Objects.requireNonNull(outboxProps, "outboxProps");
        this.outboxEntityClass = Objects.requireNonNull(outboxEntityClass, "outboxEntityClass");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public CompletableFuture<Void> publish(String topic, EventEnvelope envelope) {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(envelope, "envelope");

        if (outboxProps.getTopicPerService() != null
                && !outboxProps.getTopicPerService().isBlank()
                && !topic.equals(outboxProps.getTopicPerService())) {
            throw new IllegalArgumentException(
                    "Topic mismatch: outbox is configured for '" + outboxProps.getTopicPerService()
                            + "' but got '" + topic + "'");
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox envelope", e);
        }

        OutboxEntry row = instantiate();
        row.markPending(envelope.getAggregateId(), envelope.getEventId(),
                envelope.getEventType(), topic, payloadJson);

        entityManager.persist(row);
        log.debug("Outbox row persisted for {} (eventId={})", envelope.getEventType(), envelope.getEventId());

        // Completion semantics: the row is durable; Kafka publication is
        // asynchronous and guaranteed by the relay.
        return CompletableFuture.completedFuture(null);
    }

    private OutboxEntry instantiate() {
        try {
            var ctor = outboxEntityClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot instantiate outbox entity " + outboxEntityClass.getName(), e);
        }
    }
}
