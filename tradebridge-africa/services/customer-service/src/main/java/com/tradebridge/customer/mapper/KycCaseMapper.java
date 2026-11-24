package com.tradebridge.customer.mapper;

import com.tradebridge.customer.api.dto.response.KycCaseResponse;
import com.tradebridge.customer.api.dto.response.KycDecisionResponse;
import com.tradebridge.customer.api.dto.response.KycDocumentResponse;
import com.tradebridge.customer.api.dto.response.KycReviewResponse;
import com.tradebridge.customer.domain.KycCase;
import com.tradebridge.customer.domain.KycDecision;
import com.tradebridge.customer.domain.KycDocument;
import com.tradebridge.customer.domain.KycReview;
import org.springframework.stereotype.Component;

/**
 * Mapper for all KYC-related views. Also handles documents, reviews,
 * and decisions to keep the surface tight.
 */
@Component
public class KycCaseMapper {

    public KycCaseResponse toResponse(KycCase kycCase) {
        if (kycCase == null) return null;
        return new KycCaseResponse(
                kycCase.getId(),
                kycCase.getOrganizationId(),
                kycCase.getCaseNumber(),
                kycCase.getCaseType(),
                kycCase.getCountryCode(),
                kycCase.getCountryConfigurationVersion(),
                kycCase.getStatus().name(),
                kycCase.getRiskLevel(),
                kycCase.getAssignedReviewerId(),
                kycCase.getSubmittedAt(),
                kycCase.getCompletedAt(),
                kycCase.getCreatedAt(),
                kycCase.getUpdatedAt());
    }

    public KycDocumentResponse toDocumentResponse(KycDocument doc) {
        if (doc == null) return null;
        return new KycDocumentResponse(
                doc.getId(),
                doc.getKycCaseId(),
                doc.getDocumentType(),
                doc.getRequirementCode(),
                doc.getStorageReference(),
                doc.getOriginalFilename(),
                doc.getMimeType(),
                doc.getFileSizeBytes(),
                doc.getChecksum(),
                doc.getDocumentStatus().name(),
                doc.getUploadedByUserId(),
                doc.getUploadedAt(),
                doc.getExpiresAt());
    }

    public KycReviewResponse toReviewResponse(KycReview review) {
        if (review == null) return null;
        return new KycReviewResponse(
                review.getId(),
                review.getKycCaseId(),
                review.getReviewerUserId(),
                review.getReviewStage(),
                review.getReviewStatus(),
                review.getReviewNotes(),
                review.getStartedAt(),
                review.getCompletedAt(),
                review.getCreatedAt());
    }

    public KycDecisionResponse toDecisionResponse(KycDecision decision) {
        if (decision == null) return null;
        return new KycDecisionResponse(
                decision.getId(),
                decision.getKycCaseId(),
                decision.getDecision().name(),
                decision.getDecisionReasonCode(),
                decision.getDecisionReason(),
                decision.getDecidedByUserId(),
                decision.getDecisionVersion(),
                decision.getSupersedesDecisionId(),
                decision.getDecidedAt());
    }
}