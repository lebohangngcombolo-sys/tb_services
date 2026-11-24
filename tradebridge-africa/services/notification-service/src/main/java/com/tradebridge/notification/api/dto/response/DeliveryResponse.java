package com.tradebridge.notification.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID notificationId,
        String provider,
        String status,
        int attemptNumber,
        String failureCode,
        String failureReason,
        Instant submittedAt,
        Instant deliveredAt,
        Instant createdAt
) { }