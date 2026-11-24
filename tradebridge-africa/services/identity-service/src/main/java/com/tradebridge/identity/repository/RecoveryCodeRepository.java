package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {

    @Query("SELECT r FROM RecoveryCode r WHERE r.userId = :userId AND r.usedAt IS NULL")
    List<RecoveryCode> findUnusedByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM RecoveryCode r WHERE r.userId = :userId AND r.usedAt IS NULL")
    long countUnusedByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RecoveryCode r WHERE r.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
