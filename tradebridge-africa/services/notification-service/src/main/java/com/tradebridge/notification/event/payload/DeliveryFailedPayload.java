package com.tradebridge.notification.event.payload;

import java.util.UUID;

/**
 * {@code notification.delivery.failed.v1} payload. Consumed by
 * identity-service to record security notification delivery failures
 * (spec §3.3).
 *
 * <p>Never contains message content, OTP, or tokens.
 */
public record DeliveryFailedPayload(
        UUID notificationId,
        UUID deliveryId,
        String provider,
        String failureCode,
        String failureReason,
        UUID recipientUserId
) { }