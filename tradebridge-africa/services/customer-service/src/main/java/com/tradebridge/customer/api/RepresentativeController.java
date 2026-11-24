package com.tradebridge.customer.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.customer.api.dto.request.AddRepresentativeRequest;
import com.tradebridge.customer.api.dto.request.VerifyRepresentativeRequest;
import com.tradebridge.customer.api.dto.response.RepresentativeResponse;
import com.tradebridge.customer.service.RepresentativeService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Organization representatives (spec §6.1C).
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/representatives")
@Tag(name = "Representatives", description = "Organization representatives")
public class RepresentativeController {

    private final RepresentativeService representativeService;

    public RepresentativeController(RepresentativeService representativeService) {
        this.representativeService = representativeService;
    }

    @PostMapping
    @Operation(summary = "Add representative (authorized representative)")
    public ApiResponse<RepresentativeResponse> add(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID organizationId,
                                                    @Valid @RequestBody AddRepresentativeRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                representativeService.add(organizationId, userId, request),
                "Representative added");
    }

    @GetMapping
    @Operation(summary = "List representatives (member or staff)")
    public ApiResponse<List<RepresentativeResponse>> list(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable UUID organizationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(representativeService.list(organizationId, userId));
    }

    @PutMapping("/{representativeId}")
    @Operation(summary = "Update representative")
    public ApiResponse<RepresentativeResponse> update(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID organizationId,
                                                       @PathVariable UUID representativeId,
                                                       @Valid @RequestBody AddRepresentativeRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                representativeService.update(organizationId, representativeId, userId, request),
                "Representative updated");
    }

    @DeleteMapping("/{representativeId}")
    @Operation(summary = "Remove representative")
    public ApiResponse<Void> remove(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID organizationId,
                                     @PathVariable UUID representativeId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        representativeService.remove(organizationId, representativeId, userId);
        return ResponseFactory.ok("Representative removed");
    }

    @PatchMapping("/{representativeId}/verify")
    @Operation(summary = "Verify representative (KYC analyst)")
    @RequirePermission(PermissionCode.KYC_CASE_REVIEW)
    public ApiResponse<RepresentativeResponse> verify(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID organizationId,
                                                       @PathVariable UUID representativeId,
                                                       @Valid @RequestBody VerifyRepresentativeRequest request) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                representativeService.verify(organizationId, representativeId, request, actor),
                "Representative verified");
    }
}