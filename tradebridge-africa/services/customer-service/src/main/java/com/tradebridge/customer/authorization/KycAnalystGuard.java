package com.tradebridge.customer.authorization;

import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.security.authorization.PermissionCode;
import com.tradebridge.security.context.SecurityContextUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Guard for KYC analyst/supervisor operations (spec §6.4).
 *
 * <p>Ensures the caller has a KYC permission before any case state
 * change. The controller-level {@code @RequirePermission} is the
 * primary gate; this guard is a belt-and-suspenders check for
 * defense in depth.
 */
@Component
public class KycAnalystGuard {

    public void requireReviewPermission() {
        if (!hasAnyPermission(
                PermissionCode.KYC_CASE_REVIEW,
                PermissionCode.ROLE_KYC_ANALYST,
                PermissionCode.ROLE_ADMIN,
                PermissionCode.ROLE_SUPER_ADMIN)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSIONS,
                    "KYC review permission required");
        }
    }

    public void requireApprovePermission() {
        if (!hasAnyPermission(
                PermissionCode.KYC_CASE_APPROVE,
                PermissionCode.ROLE_KYC_SUPERVISOR,
                PermissionCode.ROLE_ADMIN,
                PermissionCode.ROLE_SUPER_ADMIN)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSIONS,
                    "KYC approval permission required");
        }
    }

    public void requireRejectPermission() {
        if (!hasAnyPermission(
                PermissionCode.KYC_CASE_REJECT,
                PermissionCode.ROLE_KYC_SUPERVISOR,
                PermissionCode.ROLE_ADMIN,
                PermissionCode.ROLE_SUPER_ADMIN)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSIONS,
                    "KYC rejection permission required");
        }
    }

    /**
     * Only the assigned reviewer (or a supervisor / admin) can complete
     * a review record.
     */
    public void requireAssignedOrSupervisor(UUID assignedReviewerId, UUID callerId) {
        if (assignedReviewerId != null && assignedReviewerId.equals(callerId)) {
            return;
        }
        if (hasAnyPermission(
                PermissionCode.ROLE_KYC_SUPERVISOR,
                PermissionCode.ROLE_ADMIN,
                PermissionCode.ROLE_SUPER_ADMIN)) {
            return;
        }
        throw new ForbiddenException(ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                "Only the assigned reviewer or a supervisor may act on this case");
    }

    private static boolean hasAnyPermission(String... permissions) {
        var authorities = SecurityContextUtils.currentAuthorities();
        for (String p : permissions) {
            if (authorities.contains(p)) return true;
        }
        return false;
    }
}