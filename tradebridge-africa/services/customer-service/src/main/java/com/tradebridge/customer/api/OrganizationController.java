package com.tradebridge.customer.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.customer.api.dto.request.CreateOrganizationRequest;
import com.tradebridge.customer.api.dto.request.UpdateOrganizationRequest;
import com.tradebridge.customer.api.dto.request.WithdrawOrganizationRequest;
import com.tradebridge.customer.api.dto.response.OrganizationResponse;
import com.tradebridge.customer.api.dto.response.OrganizationStatusResponse;
import com.tradebridge.customer.service.OrganizationService;
import com.tradebridge.customer.service.OrganizationSubmissionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Organization registration and submission (spec §6.1B).
 */
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization lifecycle and submission")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationSubmissionService submissionService;

    public OrganizationController(OrganizationService organizationService,
                                   OrganizationSubmissionService submissionService) {
        this.organizationService = organizationService;
        this.submissionService = submissionService;
    }

    @PostMapping
    @Operation(summary = "Create organization draft")
    public ApiResponse<OrganizationResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody CreateOrganizationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(organizationService.create(userId, request), "Organization created");
    }

    @GetMapping("/me")
    @Operation(summary = "List own organizations")
    public ApiResponse<List<OrganizationResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(organizationService.listForUser(userId));
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Get organization (member or staff)")
    public ApiResponse<OrganizationResponse> get(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable UUID organizationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(organizationService.get(organizationId, userId));
    }

    @PutMapping("/{organizationId}")
    @Operation(summary = "Update organization (authorized representative)")
    public ApiResponse<OrganizationResponse> update(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID organizationId,
                                                     @Valid @RequestBody UpdateOrganizationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(organizationService.update(organizationId, userId, request),
                "Organization updated");
    }

    @PostMapping("/{organizationId}/submit")
    @Operation(summary = "Submit organization for onboarding / KYC")
    public ApiResponse<OrganizationResponse> submit(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID organizationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(submissionService.submit(organizationId, userId),
                "Organization submitted");
    }

    @PostMapping("/{organizationId}/withdraw")
    @Operation(summary = "Withdraw an eligible submitted organization")
    public ApiResponse<OrganizationResponse> withdraw(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID organizationId,
                                                       @Valid @RequestBody WithdrawOrganizationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(submissionService.withdraw(organizationId, userId, request),
                "Organization withdrawn");
    }

    @GetMapping("/{organizationId}/status")
    @Operation(summary = "Get onboarding status (member or staff)")
    public ApiResponse<OrganizationStatusResponse> status(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable UUID organizationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(organizationService.status(organizationId, userId));
    }
}