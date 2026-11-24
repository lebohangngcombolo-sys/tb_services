package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.pagination.PageRequest;
import com.tradebridge.common.pagination.PageMetadata;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.identity.api.dto.request.UpdateUserStatusRequest;
import com.tradebridge.identity.api.dto.response.UserProfileResponse;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.Role;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserRole;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.domain.enums.UserStatus;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.mapper.UserMapper;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User administration (spec §3.1E): search, view, status change, unlock.
 */
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final IdentityAuditRecordRepository auditRepository;
    private final IdentityEventPublisher eventPublisher;
    private final UserMapper userMapper;

    public UserAdminService(UserRepository userRepository,
                            UserCredentialRepository credentialRepository,
                            UserRoleRepository userRoleRepository,
                            RoleRepository roleRepository,
                            IdentityAuditRecordRepository auditRepository,
                            IdentityEventPublisher eventPublisher,
                            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.auditRepository = auditRepository;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> search(String query, PageRequest pageRequest) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        org.springframework.data.domain.PageRequest springPage = org.springframework.data.domain.PageRequest
                .of(pageRequest.getPage(), pageRequest.getSize(), sort);

        Page<User> page = (query == null || query.isBlank())
                ? userRepository.findAll(springPage)
                : userRepository.search(query.trim(), springPage);

        List<UserProfileResponse> content = page.getContent().stream()
                .map(this::toProfile)
                .toList();

        PageMetadata meta = PageMetadata.of(page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return toProfile(user);
    }

    @Transactional
    public void updateStatus(UUID userId, UpdateUserStatusRequest request, UUID actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        UserStatus oldStatus = user.getStatus();
        UserStatus newStatus = UserStatus.valueOf(request.status());
        if (oldStatus == newStatus) {
            return;
        }
        switch (newStatus) {
            case ACTIVE -> user.activate();
            case SUSPENDED -> user.suspend();
            case LOCKED -> user.lock();
            case CLOSED -> user.close();
            default -> throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Unsupported status transition");
        }
        userRepository.save(user);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishStatusChanged(userId, oldStatus.name(), newStatus.name(), correlationId);

        auditRepository.save(new IdentityAuditRecord(userId, actorId,
                IdentityAction.USER_STATUS_CHANGED, null,
                Map.of("from", oldStatus.name(), "to", newStatus.name(),
                        "reason", request.reason() == null ? "" : request.reason())));
    }

    @Transactional
    public void unlock(UUID userId, UUID actorId) {
        credentialRepository.findByUserId(userId).ifPresent(creds -> {
            creds.resetFailedAttempts();
            credentialRepository.save(creds);
        });
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getStatus() == UserStatus.LOCKED) {
                user.unlock();
                userRepository.save(user);
                UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
                eventPublisher.publishStatusChanged(userId,
                        UserStatus.LOCKED.name(), UserStatus.ACTIVE.name(), correlationId);
            }
        });
        auditRepository.save(new IdentityAuditRecord(
                userId, actorId, IdentityAction.USER_UNLOCKED, null, Map.of()));
    }

    private UserProfileResponse toProfile(User user) {
        List<String> roles = userRoleRepository
                .findActiveByUserId(user.getId(), Instant.now())
                .stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()).map(Role::getCode).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return userMapper.toProfile(user, roles);
    }
}
