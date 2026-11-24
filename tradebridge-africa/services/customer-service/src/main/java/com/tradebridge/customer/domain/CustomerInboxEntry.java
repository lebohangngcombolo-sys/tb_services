package com.tradebridge.customer.domain;

import com.tradebridge.messaging.inbox.InboxEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete inbox entity for idempotent consumption (spec §1.4).
 * Table {@code customer_inbox} created by {@code V3__outbox_inbox.sql}.
 */
@Entity
@Table(name = "customer_inbox")
public class CustomerInboxEntry extends InboxEntry {
}