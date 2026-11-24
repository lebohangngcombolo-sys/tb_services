package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.identity.api.dto.response.RoleResponse;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.Role;
import com.tradebridge.identity.domain.UserRole;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.RefreshTokenRepository;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import com.tradebridge.identity.repository.UserSessionRepository;
import com.tradebridge.security.authorization.PermissionCode;
import com.tradebridge.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Role assignment and role change propagation (spec §3.1E, §3.4).
 *
 * <p>Rule: "A user cannot assign roles they are not authorized to grant."
 * Only SUPER_ADMIN may assign SUPER_ADMIN. Role changes are audited and
 * invalidate sessions so cached JWT roles are refreshed (spec §3.1E).
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IdentityAuditRecordRepository auditRepository;
    private final IdentityEventPublisher eventPublisher;

    public RoleService(RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       UserRepository userRepository,
                       UserSessionRepository sessionRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       IdentityAuditRecordRepository auditRepository,
                       IdentityEventPublisher eventPublisher) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditRepository = auditRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listAll() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleResponse(r.getId(), r.getCode(), r.getName(),
                        r.getDescription(), r.isSystemRole()))
                .toList();
    }

    @Transactional
    public void assignRoles(UUID userId, List<String> roleCodes, UUID actorId) {
        // Validate actor's authority
        Set<String> actorRoles = SecurityContextUtils.currentAuthorities();
        boolean isSuperAdmin = actorRoles.contains(PermissionCode.ROLE_SUPER_ADMIN);
        boolean isAdmin = isSuperAdmin || actorRoles.contains(PermissionCode.ROLE_ADMIN);
        if (!isAdmin) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSIONS,
                    "Only administrators may assign roles");
        }
        if (roleCodes.contains(PermissionCode.ROLE_SUPER_ADMIN) && !isSuperAdmin) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSIONS,
                    "Only SUPER_ADMIN may grant SUPER_ADMIN");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        List<Role> roles = roleRepository.findByCodeIn(roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "One or more roles do not exist");
        }

        // Replace all current assignments with the new set
        userRoleRepository.deleteByUserId(userId);
        Instant now = Instant.now();
        for (Role role : roles) {
            userRoleRepository.save(new UserRole(userId, role.getId(), actorId, null));
        }

        // Invalidate sessions so JWT roles refresh
        sessionRepository.revokeAllByUserId(userId, now);
        refreshTokenRepository.revokeAllByUserId(userId, now);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishRoleChanged(userId, roleCodes, correlationId);

        auditRepository.save(new IdentityAuditRecord(
                userId, actorId, IdentityAction.ROLE_ASSIGNED, null,
                Map.of("roleCodes", roleCodes)));
    }
}
