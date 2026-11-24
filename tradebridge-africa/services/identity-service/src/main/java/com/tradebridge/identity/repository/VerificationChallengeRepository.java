package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.VerificationChallenge;
import com.tradebridge.identity.domain.enums.ChallengeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationChallengeRepository extends JpaRepository<VerificationChallenge, UUID> {

    @Query("SELECT c FROM VerificationChallenge c WHERE c.userId = :userId "
            + "AND c.challengeType = :type AND c.consumedAt IS NULL "
            + "AND c.expiresAt > :now ORDER BY c.createdAt DESC")
    List<VerificationChallenge> findActiveByUserAndType(@Param("userId") UUID userId,
                                                        @Param("type") ChallengeType type,
                                                        @Param("now") Instant now);

    Optional<VerificationChallenge> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT MAX(c.createdAt) FROM VerificationChallenge c "
            + "WHERE c.userId = :userId AND c.challengeType = :type")
    Optional<Instant> lastIssuedAt(@Param("userId") UUID userId, @Param("type") ChallengeType type);
}
