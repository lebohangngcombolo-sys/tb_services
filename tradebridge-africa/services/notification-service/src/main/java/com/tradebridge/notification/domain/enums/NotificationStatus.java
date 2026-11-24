package com.tradebridge.notification.domain.enums;

/**
 * Notification lifecycle (spec §5.2 {@code notifications.status}).
 *
 * <p>State flow:
 * <pre>
 *   PENDING → SCHEDULED → SENT → DELIVERED
 *                       ↘ FAILED → (retry) → SENT
 *                              ↘ EXHAUSTED  (retry budget exhausted)
 * </pre>
 *
 * <p>ACCEPTED by a provider is not the same as DELIVERED — see
 * {@link DeliveryStatus} for per-attempt outcomes.
 */
public enum NotificationStatus {
    PENDING,
    SCHEDULED,
    SENT,
    DELIVERED,
    FAILED,
    EXHAUSTED
}