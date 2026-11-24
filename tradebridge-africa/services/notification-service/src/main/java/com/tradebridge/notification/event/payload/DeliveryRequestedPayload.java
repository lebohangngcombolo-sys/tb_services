package com.tradebridge.notification.event.payload;

import java.util.UUID;

/**
 * {@code notification.delivery.requested.v1} payload — published when a
 * delivery attempt is scheduled.
 */
public record DeliveryRequestedPayload(
        UUID notificationId,
        String channel,
        String templateCode,
        String provider
) { }