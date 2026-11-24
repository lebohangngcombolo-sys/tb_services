package com.tradebridge.notification.service;

import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.notification.api.dto.response.NotificationStatusResponse;
import com.tradebridge.notification.domain.Notification;
import com.tradebridge.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Thin query service used by internal callers (e.g. other services)
 * to look up notification status without going through the customer-facing
 * "me" endpoints.
 */
@Service
public class NotificationQueryService {

    private final NotificationRepository repository;

    public NotificationQueryService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotificationStatusResponse get(UUID notificationId) {
        Notification n = repository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        return new NotificationStatusResponse(
                n.getId(), n.getChannel().name(), n.getStatus().name(),
                n.getTemplateCode(), n.getCreatedAt(), n.getScheduledAt());
    }
}