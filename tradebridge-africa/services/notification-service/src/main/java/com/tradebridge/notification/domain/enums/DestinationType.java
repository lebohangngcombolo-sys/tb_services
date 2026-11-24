package com.tradebridge.notification.domain.enums;

/**
 * Recipient destination type (spec §5.2 {@code notification_recipients}).
 *
 * <p>Matches {@code Channel} but exists separately because IN_APP is
 * conceptually a destination type (the recipient is a user ID, not an
 * address) while EMAIL/SMS are addresses.
 */
public enum DestinationType {
    EMAIL,
    SMS,
    IN_APP
}