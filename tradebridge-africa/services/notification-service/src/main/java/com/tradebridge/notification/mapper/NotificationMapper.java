package com.tradebridge.notification.mapper;

import com.tradebridge.notification.api.dto.response.NotificationResponse;
import com.tradebridge.notification.api.dto.response.NotificationStatusResponse;
import com.tradebridge.notification.domain.Notification;
import com.tradebridge.notification.domain.NotificationRecipient;
import com.tradebridge.notification.domain.enums.NotificationStatus;

/**
 * Stateless mapper for notification views.
 */
public final class NotificationMapper {

    public NotificationResponse toResponse(Notification n, NotificationRecipient recipient) {
        if (n == null) return null;
        boolean read = n.getStatus() == NotificationStatus.DELIVERED;
        return new NotificationResponse(
                n.getId(),
                n.getChannel().name(),
                n.getSubject(),
                bodyPreview(n, recipient),
                n.getStatus().name(),
                n.getPriority().name(),
                read,
                n.getCreatedAt(),
                read ? n.getUpdatedAt() : null);
    }

    public NotificationStatusResponse toStatusResponse(Notification n) {
        if (n == null) return null;
        return new NotificationStatusResponse(
                n.getId(),
                n.getChannel().name(),
                n.getStatus().name(),
                n.getTemplateCode(),
                n.getCreatedAt(),
                n.getScheduledAt());
    }

    /**
     * Returns a short, non-sensitive body preview. When the notification is
     * OTP-sensitive (no body stored) we return null.
     */
    private static String bodyPreview(Notification n, NotificationRecipient recipient) {
        if (n.getBodyReference() == null) {
            return null;
        }
        return "content stored securely";
    }
}