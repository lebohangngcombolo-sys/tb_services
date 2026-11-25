package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.UserSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId "
            + "AND s.revokedAt IS NULL AND s.expiresAt > :now "
            + "ORDER BY s.lastActivityAt DESC")
    List<UserSession> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserSession s WHERE s.id = :id")
    Optional<UserSession> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE UserSession s SET s.revokedAt = :when "
            + "WHERE s.userId = :userId AND s.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("when") Instant when);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId "
            + "AND s.revokedAt IS NULL AND s.expiresAt > :now")
    long countActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId "
            + "AND s.revokedAt IS NULL AND s.expiresAt > :now "
            + "ORDER BY s.lastActivityAt ASC")
    List<UserSession> findOldestActiveByUserId(@Param("userId") UUID userId,
                                                @Param("now") Instant now);
}
