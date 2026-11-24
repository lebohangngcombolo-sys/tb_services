package com.tradebridge.country.repository;

import com.tradebridge.country.domain.KycRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycRequirementRepository extends JpaRepository<KycRequirement, UUID> {

    List<KycRequirement> findByCountryCode(String countryCode);

    List<KycRequirement> findByCountryCodeAndEnabledTrue(String countryCode);

    List<KycRequirement> findByCountryCodeAndCustomerTypeAndEnabledTrue(
            String countryCode, String customerType);

    Optional<KycRequirement> findByCountryCodeAndCustomerTypeAndRequirementCode(
            String countryCode, String customerType, String requirementCode);

    boolean existsByCountryCodeAndCustomerTypeAndRequirementCode(
            String countryCode, String customerType, String requirementCode);
}