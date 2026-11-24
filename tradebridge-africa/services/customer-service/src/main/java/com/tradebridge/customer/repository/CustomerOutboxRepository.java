package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.CustomerOutboxEntry;
import com.tradebridge.messaging.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Optional typed repository for the customer outbox.
 */
@Repository
public interface CustomerOutboxRepository extends JpaRepository<CustomerOutboxEntry, UUID> {

    List<CustomerOutboxEntry> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}