package com.tradebridge.notification.repository;

import com.tradebridge.messaging.outbox.OutboxStatus;
import com.tradebridge.notification.domain.NotificationOutboxEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Optional typed repository for the notification outbox. The
 * {@code OutboxRelay} from the messaging-starter uses
 * {@code EntityManager}, not this repository, but this interface is
 * handy for tests and admin queries.
 */
@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEntry, UUID> {

    List<NotificationOutboxEntry> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}