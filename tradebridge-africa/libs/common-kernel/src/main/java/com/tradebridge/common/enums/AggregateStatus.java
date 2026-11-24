package com.tradebridge.common.enums;

/**
 * Common aggregate lifecycle statuses shared across services.
 * Business-specific status enums should mirror where sensible but
 * stay inside their owning service.
 */
public enum AggregateStatus {
    DRAFT,
    PENDING,
    ACTIVE,
    SUSPENDED,
    CLOSED,
    WITHDRAWN,
    REJECTED
}