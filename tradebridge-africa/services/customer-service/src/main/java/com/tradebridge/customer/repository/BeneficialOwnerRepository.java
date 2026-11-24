package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.BeneficialOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficialOwnerRepository extends JpaRepository<BeneficialOwner, UUID> {

    List<BeneficialOwner> findByOrganizationId(UUID organizationId);

    Optional<BeneficialOwner> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT COALESCE(SUM(b.ownershipPercentage), 0) FROM BeneficialOwner b "
            + "WHERE b.organizationId = :organizationId "
            + "AND b.controlType IN ('DIRECT', 'INDIRECT')")
    BigDecimal sumOwnershipPercentage(@Param("organizationId") UUID organizationId);

    long countByOrganizationId(UUID organizationId);
}