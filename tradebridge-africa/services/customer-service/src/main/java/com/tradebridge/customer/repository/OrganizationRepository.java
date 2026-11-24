package com.tradebridge.customer.repository;

import com.tradebridge.customer.domain.Organization;
import com.tradebridge.customer.domain.enums.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByOrganizationNumber(String number);

    /**
     * Organizations the user can access: those they created, or where
     * they are listed as an active representative (via identity_user_id).
     */
    @Query("""
            SELECT DISTINCT o FROM Organization o
            WHERE o.createdByUserId = :userId
               OR o.id IN (SELECT r.organizationId FROM OrganizationRepresentative r
                           WHERE r.identityUserId = :userId)
            """)
    List<Organization> findAccessibleByUser(@Param("userId") UUID userId);

    @Query("""
            SELECT o FROM Organization o
            WHERE o.countryCode = :countryCode
              AND o.registrationAuthority = :authority
              AND o.registrationNumber = :registrationNumber
            """)
    Optional<Organization> findDuplicate(@Param("countryCode") String countryCode,
                                          @Param("authority") String authority,
                                          @Param("registrationNumber") String registrationNumber);

    List<Organization> findByStatus(OrganizationStatus status);
}