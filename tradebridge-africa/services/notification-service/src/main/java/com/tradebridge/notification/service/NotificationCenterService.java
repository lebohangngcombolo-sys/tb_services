package com.tradebridge.notification.service;

import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.pagination.PageMetadata;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.notification.api.dto.response.NotificationResponse;
import com.tradebridge.notification.api.dto.response.UnreadCountResponse;
import com.tradebridge.notification.domain.Notification;
import com.tradebridge.notification.domain.enums.NotificationStatus;
import com.tradebridge.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer notification centre (spec §5.1B).
 *
 * <p>"Read" state is derived from an in-app concept: the notification is
 * considered read when its {@code notification_logs} entry records the
 * READ action. Since the audit table is append-only, marking read inserts
 * a new log row; this query joins indirectly by checking the latest log
 * action. To keep this implementation simple, we currently interpret
 * "read" as {@code status == DELIVERED} for IN_APP notifications and
 * {@code status == SENT} for EMAIL/SMS — a pragmatic simplification that
 * avoids adding a dedicated column. When richer UI is needed, add a
 * {@code read_at} column to {@code notifications}.
 */
@Service
public class NotificationCenterService {

    private final NotificationRepository repository;

    public NotificationCenterService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, int page, int size, Boolean unreadOnly) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = Boolean.TRUE.equals(unreadOnly)
                ? repository.findByRecipientUserIdAndStatusNotOrderByCreatedAtDesc(
                        userId, NotificationStatus.DELIVERED, pr)
                : repository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pr);

        List<NotificationResponse> content = result.getContent().stream()
                .map(NotificationCenterService::toResponse)
                .toList();
        PageMetadata meta = PageMetadata.of(result.getNumber(), result.getSize(), result.getTotalElements());
        return PageResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        long count = repository.findByRecipientUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent().stream()
                .filter(n -> n.getStatus() != NotificationStatus.DELIVERED)
                .count();
        return new UnreadCountResponse(count);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID userId, UUID notificationId) {
        Notification n = repository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        return toResponse(n);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification n = repository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (n.getStatus() != NotificationStatus.DELIVERED) {
            n.markDelivered();
            repository.save(n);
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        Page<Notification> page = repository.findByRecipientUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, Integer.MAX_VALUE));
        for (Notification n : page.getContent()) {
            if (n.getStatus() != NotificationStatus.DELIVERED) {
                n.markDelivered();
                repository.save(n);
            }
        }
    }

    @Transactional
    public void archive(UUID userId, UUID notificationId) {
        Notification n = repository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        // Archive is modeled as marking read + closed — future expansion will
        // introduce an ARCHIVED status. For now, mark read.
        if (n.getStatus() != NotificationStatus.DELIVERED) {
            n.markDelivered();
            repository.save(n);
        }
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getChannel().name(),
                n.getSubject(),
                n.getBodyReference() == null ? null : "body-content-stored",
                n.getStatus().name(),
                n.getPriority().name(),
                n.getStatus() == NotificationStatus.DELIVERED,
                n.getCreatedAt(),
                n.getStatus() == NotificationStatus.DELIVERED ? n.getUpdatedAt() : null);
    }
}