package com.tradebridge.notification.repository;

import com.tradebridge.notification.domain.Notification;
import com.tradebridge.notification.domain.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndStatusNotOrderByCreatedAtDesc(
            UUID recipientUserId, NotificationStatus excludedStatus, Pageable pageable);

    List<Notification> findByStatusOrderByCreatedAtAsc(NotificationStatus status);
}