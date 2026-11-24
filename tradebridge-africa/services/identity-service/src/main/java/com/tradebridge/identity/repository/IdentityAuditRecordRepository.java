package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.enums.IdentityAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IdentityAuditRecordRepository extends JpaRepository<IdentityAuditRecord, UUID> {

    Page<IdentityAuditRecord> findByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);

    List<IdentityAuditRecord> findByUserIdAndActionOrderByOccurredAtDesc(UUID userId, IdentityAction action);

    Page<IdentityAuditRecord> findByOccurredAtBetween(Instant from, Instant to, Pageable pageable);
}
