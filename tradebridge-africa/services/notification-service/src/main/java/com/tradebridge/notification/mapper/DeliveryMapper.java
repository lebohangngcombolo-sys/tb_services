package com.tradebridge.notification.mapper;

import com.tradebridge.notification.api.dto.response.DeliveryResponse;
import com.tradebridge.notification.domain.NotificationDelivery;

public final class DeliveryMapper {

    public DeliveryResponse toResponse(NotificationDelivery d) {
        if (d == null) return null;
        return new DeliveryResponse(
                d.getId(),
                d.getNotificationId(),
                d.getProvider(),
                d.getStatus().name(),
                d.getAttemptNumber(),
                d.getFailureCode(),
                d.getFailureReason(),
                d.getSubmittedAt(),
                d.getDeliveredAt(),
                d.getCreatedAt());
    }
}