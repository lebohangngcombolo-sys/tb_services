package com.tradebridge.messaging.producer;

import com.tradebridge.messaging.event.EventEnvelope;

import java.util.concurrent.CompletableFuture;

/**
 * Contract for publishing a fully-formed envelope to a topic.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link KafkaEventPublisher} — fire-and-forget to Kafka (used for
 *       non-critical / telemetry events).</li>
 *   <li>{@link TransactionalEventPublisher} — persists to the outbox table
 *       in the same transaction as the domain change; a scheduled relay
 *       publishes to Kafka. <b>This is the mandatory pattern for all
 *       business events</b> per spec §1.4.</li>
 * </ul>
 */
public interface EventPublisher {

    /**
     * Publish an envelope to a topic. For the transactional implementation,
     * the outbox row is saved (subject to the current transaction) and the
     * actual Kafka write is deferred to the relay.
     */
    CompletableFuture<Void> publish(String topic, EventEnvelope envelope);
}
