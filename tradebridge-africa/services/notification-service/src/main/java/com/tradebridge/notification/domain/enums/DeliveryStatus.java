package com.tradebridge.notification.domain.enums;

/**
 * Per-attempt delivery status (spec §5.3/§5.4).
 *
 * <p>A provider accepting a message (ACCEPTED) is not the same as the
 * recipient receiving it (DELIVERED). Provider callbacks transition
 * ACCEPTED → DELIVERED / BOUNCED.
 */
public enum DeliveryStatus {
    ACCEPTED,
    DELIVERED,
    BOUNCED,
    FAILED
}