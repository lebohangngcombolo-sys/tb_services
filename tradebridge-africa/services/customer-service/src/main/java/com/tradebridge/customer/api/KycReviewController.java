package com.tradebridge.customer.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.customer.api.dto.request.CompleteKycReviewRequest;
import com.tradebridge.customer.api.dto.request.CreateKycReviewRequest;
import com.tradebridge.customer.api.dto.response.KycReviewResponse;
import com.tradebridge.customer.service.KycReviewService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * KYC reviews (spec §6.1F).
 */
@RestController
@RequestMapping("/api/v1/kyc/cases/{caseId}/reviews")
@Tag(name = "KYC Reviews", description = "Review history and workflow")
public class KycReviewController {

    private final KycReviewService reviewService;

    public KycReviewController(KycReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @Operation(summary = "Create a review record (KYC analyst)")
    @RequirePermission(PermissionCode.KYC_CASE_REVIEW)
    public ApiResponse<KycReviewResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable UUID caseId,
                                                   @Valid @RequestBody CreateKycReviewRequest request) {
        UUID analystId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(reviewService.create(caseId, analystId, request),
                "Review created");
    }

    @GetMapping
    @Operation(summary = "List reviews (KYC analyst or staff)")
    @RequirePermission(PermissionCode.KYC_CASE_READ)
    public ApiResponse<List<KycReviewResponse>> list(@PathVariable UUID caseId) {
        return ResponseFactory.ok(reviewService.list(caseId));
    }

    @PostMapping("/{reviewId}/complete")
    @Operation(summary = "Complete a review (assigned reviewer)")
    @RequirePermission(PermissionCode.KYC_CASE_REVIEW)
    public ApiResponse<KycReviewResponse> complete(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID caseId,
                                                    @PathVariable UUID reviewId,
                                                    @Valid @RequestBody CompleteKycReviewRequest request) {
        UUID analystId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                reviewService.complete(caseId, reviewId, analystId, request),
                "Review completed");
    }
}