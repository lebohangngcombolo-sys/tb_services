package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.KycReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycReviewRepository extends JpaRepository<KycReview, UUID> {

    List<KycReview> findByKycCaseIdOrderByCreatedAtDesc(UUID kycCaseId);

    Optional<KycReview> findByIdAndKycCaseId(UUID id, UUID kycCaseId);

    List<KycReview> findByKycCaseIdAndReviewStatus(UUID kycCaseId, String reviewStatus);
}