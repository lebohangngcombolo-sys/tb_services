package com.tradebridge.notification.repository;

import com.tradebridge.notification.domain.NotificationDelivery;
import com.tradebridge.notification.domain.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    List<NotificationDelivery> findByNotificationIdOrderByAttemptNumberAsc(UUID notificationId);

    Optional<NotificationDelivery> findFirstByNotificationIdOrderByAttemptNumberDesc(UUID notificationId);

    @Query("SELECT MAX(d.attemptNumber) FROM NotificationDelivery d WHERE d.notificationId = :notificationId")
    Optional<Integer> findMaxAttemptNumber(@Param("notificationId") UUID notificationId);

    List<NotificationDelivery> findByStatus(DeliveryStatus status);
}