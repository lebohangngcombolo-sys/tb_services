package com.tradebridge.notification.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.notification.api.dto.request.BulkSubmitRequest;
import com.tradebridge.notification.api.dto.request.SubmitEmailRequest;
import com.tradebridge.notification.api.dto.request.SubmitInAppRequest;
import com.tradebridge.notification.api.dto.request.SubmitNotificationRequest;
import com.tradebridge.notification.api.dto.request.SubmitSmsRequest;
import com.tradebridge.notification.api.dto.response.NotificationStatusResponse;
import com.tradebridge.notification.service.NotificationSubmissionService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Internal submission endpoints (spec §5.1A).
 *
 * <p>Requires the {@code notification.submit} permission, granted to
 * service accounts (per {@code PermissionCode}). These are NOT exposed
 * to ordinary customers by the Gateway.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification Submission", description = "Internal submission endpoints")
public class NotificationSubmissionController {

    private final NotificationSubmissionService submissionService;

    public NotificationSubmissionController(NotificationSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @Operation(summary = "Submit a notification (any channel)")
    @RequirePermission(PermissionCode.NOTIFICATION_SUBMIT)
    public ApiResponse<NotificationStatusResponse> submit(
            @Valid @RequestBody SubmitNotificationRequest request) {
        return ResponseFactory.ok(submissionService.submit(request), "Notification submitted");
    }

    @PostMapping("/email")
    @Operation(summary = "Submit an email notification")
    @RequirePermission(PermissionCode.NOTIFICATION_SUBMIT)
    public ApiResponse<NotificationStatusResponse> submitEmail(
            @Valid @RequestBody SubmitEmailRequest request) {
        return ResponseFactory.ok(submissionService.submitEmail(request), "Email submitted");
    }

    @PostMapping("/sms")
    @Operation(summary = "Submit an SMS notification")
    @RequirePermission(PermissionCode.NOTIFICATION_SUBMIT)
    public ApiResponse<NotificationStatusResponse> submitSms(
            @Valid @RequestBody SubmitSmsRequest request) {
        return ResponseFactory.ok(submissionService.submitSms(request), "SMS submitted");
    }

    @PostMapping("/in-app")
    @Operation(summary = "Submit an in-app notification")
    @RequirePermission(PermissionCode.NOTIFICATION_SUBMIT)
    public ApiResponse<NotificationStatusResponse> submitInApp(
            @Valid @RequestBody SubmitInAppRequest request) {
        return ResponseFactory.ok(submissionService.submitInApp(request), "In-app notification created");
    }

    @PostMapping("/bulk")
    @Operation(summary = "Submit a bulk batch of notifications")
    @RequirePermission(PermissionCode.NOTIFICATION_SUBMIT)
    public ApiResponse<List<NotificationStatusResponse>> submitBulk(
            @Valid @RequestBody BulkSubmitRequest request) {
        return ResponseFactory.ok(submissionService.submitBulk(request), "Bulk submitted");
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification status")
    @RequirePermission(PermissionCode.NOTIFICATION_READ)
    public ApiResponse<NotificationStatusResponse> get(@PathVariable UUID notificationId) {
        return ResponseFactory.ok(submissionService.getStatus(notificationId));
    }
}