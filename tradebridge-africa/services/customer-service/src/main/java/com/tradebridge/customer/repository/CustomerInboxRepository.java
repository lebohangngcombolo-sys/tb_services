package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.CustomerInboxEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Optional typed repository for the customer inbox.
 */
@Repository
public interface CustomerInboxRepository extends JpaRepository<CustomerInboxEntry, UUID> {

    Optional<CustomerInboxEntry> findByEventIdAndConsumerName(UUID eventId, String consumerName);
}