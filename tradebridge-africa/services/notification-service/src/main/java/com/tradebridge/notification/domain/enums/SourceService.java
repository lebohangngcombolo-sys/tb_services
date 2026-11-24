package com.tradebridge.notification.domain.enums;

/**
 * Source service identifiers for notifications (spec §5.2
 * {@code notifications.source_service}).
 *
 * <p>Values correspond to the four services that submit notifications
 * plus SYSTEM for internal scheduled messages.
 */
public enum SourceService {
    IDENTITY,
    CUSTOMER,
    COUNTRY,
    SYSTEM
}