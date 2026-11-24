package com.tradebridge.customer.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.customer.api.dto.request.AddBeneficialOwnerRequest;
import com.tradebridge.customer.api.dto.request.OwnershipDeclarationRequest;
import com.tradebridge.customer.api.dto.response.BeneficialOwnerResponse;
import com.tradebridge.customer.service.BeneficialOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Beneficial ownership (spec §6.1D).
 */
// Add to BeneficialOwnerController — a second mapping bound to the
// canonical path (spec §6.1D "POST /organizations/{organizationId}/ownership/submit")
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/ownership")
@Tag(name = "Ownership Declaration", description = "Ownership declaration submission")
class OwnershipDeclarationController {

    private final BeneficialOwnerService ownerService;

    OwnershipDeclarationController(BeneficialOwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit ownership declaration")
    public ApiResponse<Void> submit(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID organizationId,
                                     @Valid @RequestBody OwnershipDeclarationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ownerService.submitDeclaration(organizationId, userId, request);
        return ResponseFactory.ok("Ownership declaration submitted");
    }


    @PostMapping
    @Operation(summary = "Add beneficial owner")
    public ApiResponse<BeneficialOwnerResponse> add(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID organizationId,
                                                     @Valid @RequestBody AddBeneficialOwnerRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(ownerService.add(organizationId, userId, request),
                "Beneficial owner added");
    }

    @GetMapping
    @Operation(summary = "List beneficial owners (member or staff)")
    public ApiResponse<List<BeneficialOwnerResponse>> list(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable UUID organizationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(ownerService.list(organizationId, userId));
    }

    @GetMapping("/{ownerId}")
    @Operation(summary = "Get a beneficial owner")
    public ApiResponse<BeneficialOwnerResponse> get(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID organizationId,
                                                     @PathVariable UUID ownerId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(ownerService.get(organizationId, ownerId, userId));
    }

    @PutMapping("/{ownerId}")
    @Operation(summary = "Update a beneficial owner")
    public ApiResponse<BeneficialOwnerResponse> update(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable UUID organizationId,
                                                        @PathVariable UUID ownerId,
                                                        @Valid @RequestBody AddBeneficialOwnerRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                ownerService.update(organizationId, ownerId, userId, request),
                "Beneficial owner updated");
    }

    @DeleteMapping("/{ownerId}")
    @Operation(summary = "Remove a beneficial owner")
    public ApiResponse<Void> remove(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID organizationId,
                                     @PathVariable UUID ownerId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ownerService.remove(organizationId, ownerId, userId);
        return ResponseFactory.ok("Beneficial owner removed");
    }


}