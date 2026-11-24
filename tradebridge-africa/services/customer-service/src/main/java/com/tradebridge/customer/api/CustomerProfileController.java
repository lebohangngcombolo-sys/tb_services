package com.tradebridge.customer.api;

import com.tradebridge.common.pagination.PageRequest;
import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.customer.api.dto.request.CreateCustomerProfileRequest;
import com.tradebridge.customer.api.dto.request.UpdateCustomerProfileRequest;
import com.tradebridge.customer.api.dto.request.UpdateCustomerStatusRequest;
import com.tradebridge.customer.api.dto.response.CustomerProfileResponse;
import com.tradebridge.customer.service.CustomerProfileService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Individual customer profiles (spec §6.1A).
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Profile", description = "Personal profile and search")
public class CustomerProfileController {

    private final CustomerProfileService profileService;

    public CustomerProfileController(CustomerProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/me/profile")
    @Operation(summary = "Create personal profile for the authenticated customer")
    public ApiResponse<CustomerProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCustomerProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(profileService.create(userId, request), "Profile created");
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get own profile")
    public ApiResponse<CustomerProfileResponse> getOwnProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(profileService.getOwn(userId));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update own profile")
    public ApiResponse<CustomerProfileResponse> updateOwnProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCustomerProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(profileService.updateOwn(userId, request), "Profile updated");
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get any customer by ID (staff)")
    @RequirePermission(PermissionCode.CUSTOMER_READ)
    public ApiResponse<CustomerProfileResponse> get(@PathVariable UUID customerId) {
        return ResponseFactory.ok(profileService.get(customerId));
    }

    @GetMapping
    @Operation(summary = "Search customers (staff)")
    @RequirePermission(PermissionCode.CUSTOMER_READ)
    public ApiResponse<PageResponse<CustomerProfileResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseFactory.ok(profileService.search(query, countryCode, status,
                PageRequest.of(page, size)));
    }

    @PatchMapping("/{customerId}/status")
    @Operation(summary = "Update customer status (staff)")
    @RequirePermission(PermissionCode.CUSTOMER_ADMIN)
    public ApiResponse<CustomerProfileResponse> updateStatus(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(profileService.updateStatus(customerId, request, actor),
                "Status updated");
    }
}