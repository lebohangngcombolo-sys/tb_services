package com.tradebridge.notification.repository;

import com.tradebridge.notification.domain.NotificationInboxEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Optional typed repository for the notification inbox. The
 * {@code InboxGuard} from the messaging-starter uses
 * {@code EntityManager}, not this repository.
 */
@Repository
public interface NotificationInboxRepository extends JpaRepository<NotificationInboxEntry, UUID> {

    Optional<NotificationInboxEntry> findByEventIdAndConsumerName(UUID eventId, String consumerName);
}