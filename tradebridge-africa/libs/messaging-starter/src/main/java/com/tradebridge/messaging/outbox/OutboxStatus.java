package com.tradebridge.messaging.outbox;

/**
 * Lifecycle of a single outbox row.
 *
 * <ul>
 *   <li>PENDING — inserted in the same transaction as the domain change,
 *       not yet published.</li>
 *   <li>PUBLISHED — relay has successfully written the record to Kafka.</li>
 *   <li>FAILED — relay exhausted its configured attempt budget.</li>
 * </ul>
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
