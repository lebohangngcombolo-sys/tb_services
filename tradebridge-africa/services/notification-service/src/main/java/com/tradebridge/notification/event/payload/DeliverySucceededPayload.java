package com.tradebridge.notification.event.payload;

import java.util.UUID;

/**
 * {@code notification.delivery.succeeded.v1} payload — published when the
 * provider confirms successful delivery (or acceptance, depending on the
 * provider's callback capabilities; spec §5.3).
 */
public record DeliverySucceededPayload(
        UUID notificationId,
        UUID deliveryId,
        String provider,
        String providerMessageId
) { }