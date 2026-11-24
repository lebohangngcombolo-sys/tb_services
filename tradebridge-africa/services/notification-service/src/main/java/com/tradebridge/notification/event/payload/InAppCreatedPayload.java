package com.tradebridge.notification.event.payload;

import java.util.UUID;

/**
 * {@code notification.inapp.created.v1} payload — published when an in-app
 * notification is persisted for a user.
 */
public record InAppCreatedPayload(
        UUID notificationId,
        UUID recipientUserId,
        String subject
) { }