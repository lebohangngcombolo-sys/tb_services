package com.tradebridge.notification.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.notification.api.dto.request.CreateTemplateRequest;
import com.tradebridge.notification.api.dto.request.PreviewTemplateRequest;
import com.tradebridge.notification.api.dto.request.UpdateTemplateRequest;
import com.tradebridge.notification.api.dto.response.PreviewResponse;
import com.tradebridge.notification.api.dto.response.TemplateResponse;
import com.tradebridge.notification.service.TemplateService;
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

import java.util.Map;
import java.util.UUID;

/**
 * Template administration (spec §5.1D).
 */
@RestController
@RequestMapping("/api/v1/notifications/admin/templates")
@Tag(name = "Templates", description = "Notification template administration")
public class TemplateAdminController {

    private final TemplateService templateService;

    public TemplateAdminController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "List templates")
    @RequirePermission(PermissionCode.NOTIFICATION_TEMPLATE_ADMIN)
    public ApiResponse<PageResponse<TemplateResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseFactory.ok(templateService.list(page, size));
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Get a template")
    @RequirePermission(PermissionCode.NOTIFICATION_TEMPLATE_ADMIN)
    public ApiResponse<TemplateResponse> get(@PathVariable UUID templateId) {
        return ResponseFactory.ok(templateService.get(templateId));
    }

    @PostMapping
    @Operation(summary = "Create a template")
    @RequirePermission(PermissionCode.NOTIFICATION_TEMPLATE_ADMIN)
    public ApiResponse<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseFactory.ok(templateService.create(request, UUID.fromString(jwt.getSubject())),
                "Template created");
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "Update a template")
    @RequirePermission(PermissionCode.NOTIFICATION_TEMPLATE_ADMIN)
    public ApiResponse<TemplateResponse> update(@PathVariable UUID templateId,
                                                 @Valid @RequestBody UpdateTemplateRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseFactory.ok(templateService.update(templateId, request,
                UUID.fromString(jwt.getSubject())), "Template updated");
    }

    @PatchMapping("/{templateId}/status")
    @Operation(summary = "Enable or disable a template")
    @RequirePermission(PermissionCode.NOTIFICATION_TEMPLATE_ADMIN)
    public ApiResponse<TemplateResponse> changeStatus(@PathVariable UUID templateId,
                                                      @RequestBody Map<String, Boolean> body,
                                                      @AuthenticationPrincipal Jwt jwt) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ResponseFactory.ok(templateService.changeStatus(templateId, enabled,
                UUID.fromString(jwt.getSubject())), "Status updated");
    }

    @PostMapping("/{templateId}/preview")
    @Operation(summary = "Preview a rendered template")
    @RequirePermission(PermissionCode.NOTIFICATION_TEMPLATE_ADMIN)
    public ApiResponse<PreviewResponse> preview(@PathVariable UUID templateId,
                                                 @Valid @RequestBody PreviewTemplateRequest request) {
        return ResponseFactory.ok(templateService.preview(templateId, request));
    }
}