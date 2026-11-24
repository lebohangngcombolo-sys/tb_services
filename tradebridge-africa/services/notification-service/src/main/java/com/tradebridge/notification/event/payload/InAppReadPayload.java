package com.tradebridge.notification.event.payload;

import java.util.UUID;

/**
 * {@code notification.inapp.read.v1} payload — published when a user marks
 * an in-app notification as read.
 */
public record InAppReadPayload(
        UUID notificationId,
        UUID recipientUserId
) { }