package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.OrganizationRepresentative;
import com.tradebridge.customer.domain.enums.RepresentativeVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepresentativeRepository
        extends JpaRepository<OrganizationRepresentative, UUID> {

    List<OrganizationRepresentative> findByOrganizationId(UUID organizationId);

    Optional<OrganizationRepresentative> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<OrganizationRepresentative> findByOrganizationIdAndVerificationStatus(
            UUID organizationId, RepresentativeVerificationStatus status);

    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);

    @Query("SELECT COUNT(r) FROM OrganizationRepresentative r "
            + "WHERE r.organizationId = :organizationId AND r.authorizedSignatory = true")
    long countAuthorizedSignatories(@Param("organizationId") UUID organizationId);

    @Query("SELECT COUNT(r) FROM OrganizationRepresentative r "
            + "WHERE r.organizationId = :organizationId AND r.verificationStatus = 'VERIFIED'")
    long countVerified(@Param("organizationId") UUID organizationId);
}