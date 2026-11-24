package com.tradebridge.notification.event.payload;

import java.util.UUID;

/**
 * {@code notification.delivery.exhausted.v1} payload — published when the
 * retry policy has been exhausted and the notification will not be
 * attempted again (spec §5.3).
 */
public record DeliveryExhaustedPayload(
    UUID notificationId,
    int attempts
) { }
