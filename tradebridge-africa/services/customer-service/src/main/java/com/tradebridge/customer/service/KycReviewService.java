package com.tradebridge.customer.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.customer.api.dto.request.CompleteKycReviewRequest;
import com.tradebridge.customer.api.dto.request.CreateKycReviewRequest;
import com.tradebridge.customer.api.dto.response.KycReviewResponse;
import com.tradebridge.customer.domain.KycCase;
import com.tradebridge.customer.domain.KycReview;
import com.tradebridge.customer.mapper.KycCaseMapper;
import com.tradebridge.customer.repository.KycCaseRepository;
import com.tradebridge.customer.repository.KycReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * KYC reviews (spec §6.1F). Reviews are per-stage audit records. The
 * actual decision lives in {@link KycDecisionService}.
 */
@Service
public class KycReviewService {

    private final KycReviewRepository reviewRepository;
    private final KycCaseRepository kycCaseRepository;
    private final KycCaseMapper mapper;

    public KycReviewService(KycReviewRepository reviewRepository,
                             KycCaseRepository kycCaseRepository,
                             KycCaseMapper mapper) {
        this.reviewRepository = reviewRepository;
        this.kycCaseRepository = kycCaseRepository;
        this.mapper = mapper;
    }

    @Transactional
    public KycReviewResponse create(UUID caseId, UUID analystId,
                                     CreateKycReviewRequest request) {
        KycCase c = requireCase(caseId);
        if (c.getStatus().isTerminal()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Cannot create a review for a completed case");
        }
        KycReview review = new KycReview(
                UuidV7Generator.next(),
                caseId,
                analystId,
                request.reviewStage(),
                request.notes());
        reviewRepository.save(review);
        return mapper.toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public List<KycReviewResponse> list(UUID caseId) {
        requireCase(caseId);
        return reviewRepository.findByKycCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(mapper::toReviewResponse).toList();
    }

    @Transactional
    public KycReviewResponse complete(UUID caseId, UUID reviewId,
                                       UUID analystId, CompleteKycReviewRequest request) {
        KycReview review = reviewRepository.findByIdAndKycCaseId(reviewId, caseId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycReview", reviewId));
        if (!review.getReviewerUserId().equals(analystId)) {
            throw new com.tradebridge.common.exception.ForbiddenException(
                    ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                    "Only the assigned reviewer can complete this review");
        }
        if ("COMPLETED".equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Review is already completed");
        }
        review.complete(request.outcome(), request.notes());
        reviewRepository.save(review);
        return mapper.toReviewResponse(review);
    }

    private KycCase requireCase(UUID caseId) {
        return kycCaseRepository.findById(caseId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycCase", caseId));
    }
}