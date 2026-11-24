package com.tradebridge.country.repository;

import com.tradebridge.country.domain.OrganizationRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRequirementRepository extends JpaRepository<OrganizationRequirement, UUID> {

    List<OrganizationRequirement> findByCountryCode(String countryCode);

    List<OrganizationRequirement> findByCountryCodeAndEnabledTrue(String countryCode);

    Optional<OrganizationRequirement> findByCountryCodeAndOrganizationTypeAndRequirementCode(
            String countryCode, String organizationType, String requirementCode);

    boolean existsByCountryCodeAndOrganizationTypeAndRequirementCode(
            String countryCode, String organizationType, String requirementCode);

    /**
     * Requirements effective at a given instant: enabled AND effective_from &lt;= now
     * AND (effective_until IS NULL OR effective_until &gt; now).
     */
    @Query("""
            SELECT r FROM OrganizationRequirement r
            WHERE r.countryCode = :countryCode AND r.enabled = TRUE
              AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :now)
              AND (r.effectiveUntil IS NULL OR r.effectiveUntil > :now)
            """)
    List<OrganizationRequirement> findActiveByCountry(@Param("countryCode") String countryCode,
                                                      @Param("now") Instant now);
}