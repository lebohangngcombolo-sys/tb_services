package com.tradebridge.customer.api;

import com.tradebridge.common.pagination.PageRequest;
import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.customer.api.dto.request.ApproveKycRequest;
import com.tradebridge.customer.api.dto.request.AssignReviewerRequest;
import com.tradebridge.customer.api.dto.request.CreateKycCaseRequest;
import com.tradebridge.customer.api.dto.request.RegisterKycDocumentRequest;
import com.tradebridge.customer.api.dto.request.RejectKycRequest;
import com.tradebridge.customer.api.dto.request.RequestInformationRequest;
import com.tradebridge.customer.api.dto.response.KycCaseResponse;
import com.tradebridge.customer.api.dto.response.KycDocumentResponse;
import com.tradebridge.customer.api.dto.response.KycRequirementResponse;
import com.tradebridge.customer.service.KycCaseService;
import com.tradebridge.customer.service.KycDocumentService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * KYC cases and evidence (spec §6.1E).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "KYC Cases", description = "KYC case lifecycle and evidence")
public class KycCaseController {

    private final KycCaseService kycCaseService;
    private final KycDocumentService kycDocumentService;

    public KycCaseController(KycCaseService kycCaseService,
                              KycDocumentService kycDocumentService) {
        this.kycCaseService = kycCaseService;
        this.kycDocumentService = kycDocumentService;
    }

    // ------------------------------------------------------------------ create / query

    @PostMapping("/organizations/{organizationId}/kyc-cases")
    @Operation(summary = "Start a KYC case for an organization")
    public ApiResponse<KycCaseResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable UUID organizationId,
                                                @Valid @RequestBody CreateKycCaseRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycCaseService.create(organizationId, userId, request),
                "KYC case created");
    }

    @GetMapping("/organizations/{organizationId}/kyc-cases")
    @Operation(summary = "List KYC cases for an organization")
    public ApiResponse<List<KycCaseResponse>> listForOrg(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable UUID organizationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(kycCaseService.listForOrganization(organizationId, userId));
    }

    @GetMapping("/kyc/cases/{caseId}")
    @Operation(summary = "Get a KYC case")
    public ApiResponse<KycCaseResponse> get(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID caseId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(kycCaseService.get(caseId, userId));
    }

    @GetMapping("/kyc/cases/{caseId}/requirements")
    @Operation(summary = "Get required evidence for a KYC case")
    public ApiResponse<List<KycRequirementResponse>> requirements(@PathVariable UUID caseId) {
        return ResponseFactory.ok(kycCaseService.requirements(caseId));
    }

    // ------------------------------------------------------------------ documents

    @PostMapping("/kyc/cases/{caseId}/documents")
    @Operation(summary = "Register evidence metadata")
    public ApiResponse<KycDocumentResponse> registerDocument(@AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable UUID caseId,
                                                              @Valid @RequestBody RegisterKycDocumentRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycDocumentService.register(caseId, userId, request),
                "Document registered");
    }

    @GetMapping("/kyc/cases/{caseId}/documents")
    @Operation(summary = "List evidence for a KYC case")
    public ApiResponse<List<KycDocumentResponse>> listDocuments(@AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable UUID caseId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(kycDocumentService.list(caseId, userId));
    }

    @DeleteMapping("/kyc/cases/{caseId}/documents/{documentId}")
    @Operation(summary = "Remove eligible evidence")
    public ApiResponse<Void> removeDocument(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID caseId,
                                             @PathVariable UUID documentId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        kycDocumentService.remove(caseId, documentId, userId);
        return ResponseFactory.ok("Document removed");
    }

    // ------------------------------------------------------------------ submit / resubmit / request-info

    @PostMapping("/kyc/cases/{caseId}/submit")
    @Operation(summary = "Submit KYC case for review")
    public ApiResponse<KycCaseResponse> submit(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID caseId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(kycCaseService.submit(caseId, userId), "KYC case submitted");
    }

    @PostMapping("/kyc/cases/{caseId}/resubmit")
    @Operation(summary = "Resubmit a case after information request")
    public ApiResponse<KycCaseResponse> resubmit(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable UUID caseId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(kycCaseService.resubmit(caseId, userId), "KYC case resubmitted");
    }

    @PostMapping("/kyc/cases/{caseId}/request-information")
    @Operation(summary = "Request additional information (KYC analyst)")
    @RequirePermission(PermissionCode.KYC_CASE_REVIEW)
    public ApiResponse<KycCaseResponse> requestInformation(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable UUID caseId,
                                                            @Valid @RequestBody RequestInformationRequest request) {
        UUID analystId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycCaseService.requestInformation(caseId, analystId, request),
                "Information requested");
    }

    // ------------------------------------------------------------------ reviewer workflow

    @PostMapping("/kyc/cases/{caseId}/assign")
    @Operation(summary = "Assign a reviewer (KYC supervisor)")
    @RequirePermission(PermissionCode.KYC_CASE_REVIEW)
    public ApiResponse<KycCaseResponse> assign(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID caseId,
                                                 @Valid @RequestBody AssignReviewerRequest request) {
        UUID supervisorId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycCaseService.assign(caseId, supervisorId, request),
                "Reviewer assigned");
    }

    @PostMapping("/kyc/cases/{caseId}/approve")
    @Operation(summary = "Approve a KYC case")
    @RequirePermission(PermissionCode.KYC_CASE_APPROVE)
    public ApiResponse<KycCaseResponse> approve(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable UUID caseId,
                                                  @Valid @RequestBody ApproveKycRequest request) {
        UUID analystId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycCaseService.approve(caseId, analystId, request),
                "KYC case approved");
    }

    @PostMapping("/kyc/cases/{caseId}/reject")
    @Operation(summary = "Reject a KYC case")
    @RequirePermission(PermissionCode.KYC_CASE_REJECT)
    public ApiResponse<KycCaseResponse> reject(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID caseId,
                                                 @Valid @RequestBody RejectKycRequest request) {
        UUID analystId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycCaseService.reject(caseId, analystId, request),
                "KYC case rejected");
    }

    // ------------------------------------------------------------------ analyst search

    @GetMapping("/kyc/cases")
    @Operation(summary = "Search KYC cases (analyst)")
    @RequirePermission(PermissionCode.KYC_CASE_READ)
    public ApiResponse<PageResponse<KycCaseResponse>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID assignedReviewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseFactory.ok(kycCaseService.search(status, assignedReviewerId,
                PageRequest.of(page, size)));
    }
}