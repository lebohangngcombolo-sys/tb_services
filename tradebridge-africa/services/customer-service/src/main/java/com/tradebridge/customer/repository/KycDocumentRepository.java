package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByKycCaseIdOrderByUploadedAtDesc(UUID kycCaseId);

    Optional<KycDocument> findByIdAndKycCaseId(UUID id, UUID kycCaseId);

    @Query("SELECT COUNT(d) FROM KycDocument d "
            + "WHERE d.kycCaseId = :caseId "
            + "AND d.requirementCode = :requirementCode "
            + "AND d.documentStatus = 'ACCEPTED'")
    long countAcceptedForRequirement(@Param("caseId") UUID caseId,
                                     @Param("requirementCode") String requirementCode);
}