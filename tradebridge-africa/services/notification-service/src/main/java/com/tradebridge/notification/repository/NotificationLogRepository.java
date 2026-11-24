package com.tradebridge.notification.repository;

import com.tradebridge.notification.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByNotificationIdOrderByOccurredAtAsc(UUID notificationId);
}