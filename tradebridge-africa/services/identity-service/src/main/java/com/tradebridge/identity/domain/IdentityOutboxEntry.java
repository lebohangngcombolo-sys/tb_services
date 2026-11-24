package com.tradebridge.identity.domain;

import com.tradebridge.messaging.outbox.OutboxEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete outbox entity for the identity service.
 *
 * <p>Table name {@code identity_outbox} is created by
 * {@code V3__outbox_inbox.sql}. The messaging starter's {@code OutboxRelay}
 * uses this class via {@link com.tradebridge.identity.config.OutboxProvider}.
 */
@Entity
@Table(name = "identity_outbox")
public class IdentityOutboxEntry extends OutboxEntry {
}
