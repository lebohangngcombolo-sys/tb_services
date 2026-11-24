package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.KycCase;
import com.tradebridge.customer.domain.enums.KycCaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycCaseRepository extends JpaRepository<KycCase, UUID> {

    List<KycCase> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<KycCase> findByCaseNumber(String caseNumber);

    Optional<KycCase> findFirstByOrganizationIdAndStatusInOrderByCreatedAtDesc(
            UUID organizationId, List<KycCaseStatus> statuses);

    @Query("""
            SELECT c FROM KycCase c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:assignedReviewerId IS NULL OR c.assignedReviewerId = :assignedReviewerId)
            ORDER BY c.createdAt DESC
            """)
    Page<KycCase> search(@Param("status") KycCaseStatus status,
                          @Param("assignedReviewerId") UUID assignedReviewerId,
                          Pageable pageable);
}