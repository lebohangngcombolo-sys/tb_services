package com.tradebridge.customer.authorization;

import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.customer.domain.Organization;
import com.tradebridge.customer.repository.OrganizationRepository;
import com.tradebridge.customer.repository.OrganizationRepresentativeRepository;
import com.tradebridge.security.authorization.PermissionCode;
import com.tradebridge.security.context.SecurityContextUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Central authorization checks for organization resources (spec §6.4
 * "Only the authenticated user can modify their own profile", "Only
 * authorized personnel can designate a signatory").
 *
 * <p>Every mutation/read of an organization or its sub-resources
 * should route through one of these methods. They encapsulate:
 * <ul>
 *   <li>Creator check</li>
 *   <li>Representative membership check</li>
 *   <li>Staff bypass via permission checks</li>
 * </ul>
 */
@Component
public class OrganizationAccessGuard {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRepresentativeRepository representativeRepository;

    public OrganizationAccessGuard(OrganizationRepository organizationRepository,
                                    OrganizationRepresentativeRepository representativeRepository) {
        this.organizationRepository = organizationRepository;
        this.representativeRepository = representativeRepository;
    }

    public Organization requireOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", organizationId));
    }

    /**
     * Requires that the caller can read the organization:
     * creator, representative, or staff with CUSTOMER_READ.
     */
    public Organization requireReadAccess(UUID organizationId, UUID userId) {
        Organization org = requireOrganization(organizationId);
        if (org.getCreatedByUserId().equals(userId)) return org;
        boolean isRep = representativeRepository.findByOrganizationId(organizationId).stream()
                .anyMatch(r -> userId.equals(r.getIdentityUserId()));
        if (isRep) return org;
        if (hasPermission(PermissionCode.CUSTOMER_READ)
                || hasPermission(PermissionCode.KYC_CASE_READ)) return org;
        throw new ForbiddenException(ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                "User is not authorized to read this organization");
    }

    /**
     * Requires that the caller can modify the organization:
     * creator OR an active authorized representative.
     */
    public Organization requireWriteAccess(UUID organizationId, UUID userId) {
        Organization org = requireOrganization(organizationId);
        if (org.getCreatedByUserId().equals(userId)) return org;
        boolean authorizedRep = representativeRepository.findByOrganizationId(organizationId).stream()
                .anyMatch(r -> userId.equals(r.getIdentityUserId()) && r.isAuthorizedSignatory());
        if (authorizedRep) return org;
        throw new ForbiddenException(ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                "User is not an authorized representative of this organization");
    }

    /**
     * Requires that the caller can submit / withdraw the organization —
     * creator OR an active authorized signatory.
     */
    public Organization requireSubmitAccess(UUID organizationId, UUID userId) {
        return requireWriteAccess(organizationId, userId);
    }

    private static boolean hasPermission(String permission) {
        return SecurityContextUtils.currentAuthorities().contains(permission);
    }
}