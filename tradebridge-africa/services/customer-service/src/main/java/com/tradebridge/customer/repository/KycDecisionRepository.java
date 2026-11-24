package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.KycDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDecisionRepository extends JpaRepository<KycDecision, UUID> {

    List<KycDecision> findByKycCaseIdOrderByDecisionVersionDesc(UUID kycCaseId);

    Optional<KycDecision> findFirstByKycCaseIdOrderByDecisionVersionDesc(UUID kycCaseId);

    @Query("SELECT COALESCE(MAX(d.decisionVersion), 0) FROM KycDecision d "
            + "WHERE d.kycCaseId = :caseId")
    int findMaxDecisionVersion(@Param("caseId") UUID caseId);
}