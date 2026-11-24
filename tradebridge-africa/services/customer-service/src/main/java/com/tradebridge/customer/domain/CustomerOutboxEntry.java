package com.tradebridge.customer.domain;

import com.tradebridge.messaging.outbox.OutboxEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete outbox entity for the customer service (spec §1.4).
 * Table {@code customer_outbox} created by {@code V3__outbox_inbox.sql}.
 */
@Entity
@Table(name = "customer_outbox")
public class CustomerOutboxEntry extends OutboxEntry {
}