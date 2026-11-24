package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleId IN :roleIds")
    List<RolePermission> findByRoleIdIn(@Param("roleIds") Collection<UUID> roleIds);
}
