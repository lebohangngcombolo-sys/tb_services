package com.tradebridge.country.domain;

import com.tradebridge.messaging.outbox.OutboxEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete outbox entity for the country service (spec §1.4).
 * Table {@code country_outbox} created by {@code V3__outbox_inbox.sql}.
 */
@Entity
@Table(name = "country_outbox")
public class CountryOutboxEntry extends OutboxEntry {
}