package com.tradebridge.notification.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.notification.api.dto.response.DeliveryResponse;
import com.tradebridge.notification.api.dto.response.ProviderStatusResponse;
import com.tradebridge.notification.service.DeliveryService;
import com.tradebridge.notification.service.ProviderSelectionService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Delivery administration (spec §5.1E).
 */
@RestController
@RequestMapping("/api/v1/notifications/admin")
@Tag(name = "Delivery Admin", description = "Delivery and provider administration")
public class DeliveryAdminController {

    private final DeliveryService deliveryService;
    private final ProviderSelectionService providerSelectionService;

    public DeliveryAdminController(DeliveryService deliveryService,
                                    ProviderSelectionService providerSelectionService) {
        this.deliveryService = deliveryService;
        this.providerSelectionService = providerSelectionService;
    }

    @GetMapping("/deliveries")
    @Operation(summary = "Search delivery records")
    @RequirePermission(PermissionCode.NOTIFICATION_DELIVERY_ADMIN)
    public ApiResponse<PageResponse<DeliveryResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) UUID notificationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseFactory.ok(deliveryService.search(status, channel, notificationId, page, size));
    }

    @GetMapping("/deliveries/{deliveryId}")
    @Operation(summary = "Inspect a delivery")
    @RequirePermission(PermissionCode.NOTIFICATION_DELIVERY_ADMIN)
    public ApiResponse<DeliveryResponse> get(@PathVariable UUID deliveryId) {
        return ResponseFactory.ok(deliveryService.get(deliveryId));
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    @Operation(summary = "Retry an eligible failed delivery")
    @RequirePermission(PermissionCode.NOTIFICATION_DELIVERY_ADMIN)
    public ApiResponse<DeliveryResponse> retry(@PathVariable UUID deliveryId,
                                                @AuthenticationPrincipal Jwt jwt) {
        return ResponseFactory.ok(
                deliveryService.retry(deliveryId, UUID.fromString(jwt.getSubject())),
                "Retry scheduled");
    }

    @GetMapping("/providers")
    @Operation(summary = "List provider status")
    @RequirePermission(PermissionCode.NOTIFICATION_DELIVERY_ADMIN)
    public ApiResponse<List<ProviderStatusResponse>> providers() {
        return ResponseFactory.ok(providerSelectionService.providerStatuses());
    }

    @GetMapping("/health")
    @Operation(summary = "Provider health overview")
    @RequirePermission(PermissionCode.NOTIFICATION_DELIVERY_ADMIN)
    public ApiResponse<List<ProviderStatusResponse>> health() {
        return ResponseFactory.ok(providerSelectionService.providerStatuses());
    }
}