package com.tradebridge.notification.repository;

import com.tradebridge.notification.domain.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {

    List<NotificationRecipient> findByNotificationId(UUID notificationId);

    Optional<NotificationRecipient> findFirstByNotificationId(UUID notificationId);
}