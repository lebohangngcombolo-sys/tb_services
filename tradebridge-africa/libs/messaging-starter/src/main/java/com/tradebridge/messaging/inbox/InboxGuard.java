package com.tradebridge.messaging.inbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Idempotency guard for consumers.
 *
 * <p>Usage in a consumer:
 * <pre>
 * &#64;KafkaListener(topics = "identity.events")
 * public void handle(ConsumerRecord&lt;String, EventEnvelope&gt; record) {
 *     inboxGuard.executeOnce(
 *         envelope.getEventId(),
 *         "notification.identity.user.registered",
 *         () -&gt; actuallyHandle(envelope));
 * }
 * </pre>
 *
 * <p>The guard runs inside a transaction:
 * <ol>
 *   <li>Checks whether an inbox row exists — if yes, skip.</li>
 *   <li>Runs the handler (which may write its own rows).</li>
 *   <li>Writes the inbox row for this event.</li>
 * </ol>
 * If the handler throws, no inbox row is written, so Kafka can redeliver.
 */
public class InboxGuard {

    private final Class<? extends InboxEntry> inboxEntityClass;

    @PersistenceContext
    private EntityManager entityManager;

    public InboxGuard(Class<? extends InboxEntry> inboxEntityClass) {
        this.inboxEntityClass = Objects.requireNonNull(inboxEntityClass, "inboxEntityClass");
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean executeOnce(UUID eventId, String consumerName, Runnable handler) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(consumerName, "consumerName");
        Objects.requireNonNull(handler, "handler");

        String entityName = inboxEntityClass.getSimpleName();
        Long count = entityManager
                .createQuery("SELECT COUNT(i) FROM " + entityName
                        + " i WHERE i.eventId = :eventId AND i.consumerName = :name", Long.class)
                .setParameter("eventId", eventId)
                .setParameter("name", consumerName)
                .getSingleResult();

        if (count != null && count > 0) {
            return false;
        }

        handler.run();

        InboxEntry row = instantiate();
        row.initialize(eventId, consumerName, "OK");
        entityManager.persist(row);
        return true;
    }

    private InboxEntry instantiate() {
        try {
            var ctor = inboxEntityClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot instantiate inbox entity " + inboxEntityClass.getName(), e);
        }
    }
}
