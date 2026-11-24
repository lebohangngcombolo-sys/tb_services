package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.MfaFactor;
import com.tradebridge.identity.domain.enums.MfaFactorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaFactorRepository extends JpaRepository<MfaFactor, UUID> {

    Optional<MfaFactor> findByUserIdAndFactorTypeAndEnabledTrue(UUID userId, MfaFactorType factorType);

    Optional<MfaFactor> findByUserIdAndFactorType(UUID userId, MfaFactorType factorType);

    void deleteByUserId(UUID userId);
}
