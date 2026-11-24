package com.tradebridge.identity.service;

import com.tradebridge.identity.api.dto.response.PermissionResponse;
import com.tradebridge.identity.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only permission catalogue.
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listAll() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getId(), p.getCode(),
                        p.getResource(), p.getAction(), p.getDescription()))
                .toList();
    }
}
