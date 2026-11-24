package com.tradebridge.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.dlq.DeadLetterRouter;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventTypeRegistry;
import com.tradebridge.messaging.inbox.InboxGuard;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Base class for event consumers. Every service extends this with a
 * {@code @KafkaListener}-annotated method that delegates to
 * {@link #handleRecord(ConsumerRecord)}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate the event type against the {@link EventTypeRegistry}.</li>
 *   <li>Deserialize the payload to the handler's declared type.</li>
 *   <li>Wrap execution in {@link InboxGuard} for idempotency.</li>
 *   <li>Route unrecoverable failures to the DLQ via {@link DeadLetterRouter}.</li>
 *   <li>Let the listener container handle retries — this base does NOT
 *       retry; it distinguishes retryable from terminal failures.</li>
 * </ul>
 */
public abstract class BaseConsumer {

    private static final Logger log = LoggerFactory.getLogger(BaseConsumer.class);

    private final ConsumerRegistry registry;
    private final InboxGuard inboxGuard;
    private final EventTypeRegistry typeRegistry;
    private final DeadLetterRouter dlqRouter;
    private final ObjectMapper objectMapper;
    private final MessagingProperties.Consumer props;
    private final String consumerName;

    protected BaseConsumer(ConsumerRegistry registry,
                           InboxGuard inboxGuard,
                           EventTypeRegistry typeRegistry,
                           DeadLetterRouter dlqRouter,
                           ObjectMapper objectMapper,
                           MessagingProperties.Consumer props,
                           String consumerName) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.inboxGuard = inboxGuard;
        this.typeRegistry = Objects.requireNonNull(typeRegistry, "typeRegistry");
        this.dlqRouter = dlqRouter;
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.props = Objects.requireNonNull(props, "props");
        this.consumerName = Objects.requireNonNull(consumerName, "consumerName");
    }

    /**
     * Entry point. Subclasses call this from their {@code @KafkaListener}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void handleRecord(ConsumerRecord<String, EventEnvelope> record) {
        EventEnvelope envelope = record.value();
        if (envelope == null) {
            log.warn("[{}] null envelope on topic {} offset {}", consumerName, record.topic(), record.offset());
            return;
        }

        // Reject unregistered event types immediately — indicates a schema
        // mismatch that retrying will never fix.
        if (!typeRegistry.isKnown(envelope.getEventType())) {
            log.error("[{}] Unknown event type {} — routing to DLQ", consumerName, envelope.getEventType());
            dlqRouter.route(record.topic(), envelope, consumerName, "Unknown event type");
            return;
        }

        Optional<EventHandler<?>> maybeHandler = registry.find(envelope.getEventType());
        if (maybeHandler.isEmpty()) {
            // Registered type but no handler in this service — ignore quietly.
            log.debug("[{}] No handler for {} — ignoring", consumerName, envelope.getEventType());
            return;
        }

        EventHandler handler = maybeHandler.get();
        Object typedPayload;
        try {
            typedPayload = objectMapper.convertValue(envelope.getPayload(), handler.payloadType());
        } catch (Exception e) {
            log.error("[{}] Cannot deserialize payload for {}", consumerName, envelope.getEventType(), e);
            dlqRouter.route(record.topic(), envelope, consumerName, "Payload deserialization failed");
            return;
        }

        Runnable task = () -> handler.handle(envelope, typedPayload);

        try {
            if (inboxGuard != null) {
                boolean fresh = inboxGuard.executeOnce(
                        envelope.getEventId(), consumerName, task);
                if (!fresh) {
                    log.debug("[{}] Duplicate event {} — skipped", consumerName, envelope.getEventId());
                }
            } else {
                task.run();
            }
        } catch (RuntimeException ex) {
            // Distinguish terminal business errors from transient failures.
            // For now, any RuntimeException triggers DLQ routing on the next
            // redelivery attempt — the listener error handler should call
            // `handleTerminal` after max retries.
            log.warn("[{}] Handler failed for {}: {}", consumerName, envelope.getEventType(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Subclasses or error handlers call this when retry budget is exhausted
     * for a record.
     */
    public void handleTerminal(ConsumerRecord<String, EventEnvelope> record, Throwable cause) {
        if (record.value() == null) return;
        dlqRouter.route(record.topic(), record.value(), consumerName,
                cause == null ? "unknown" : cause.getMessage());
    }

    public String consumerName() {
        return consumerName;
    }
}
