package com.tradebridge.country.repository;

import com.tradebridge.country.domain.CountryConfiguration;
import com.tradebridge.country.domain.enums.ConfigurationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountryConfigurationRepository extends JpaRepository<CountryConfiguration, UUID> {

    List<CountryConfiguration> findByCountryCodeAndVersion(String countryCode, int version);

    List<CountryConfiguration> findByCountryCodeAndVersionAndStatus(
            String countryCode, int version, ConfigurationStatus status);

    Optional<CountryConfiguration> findByCountryCodeAndConfigurationKeyAndVersion(
            String countryCode, String configurationKey, int version);

    List<CountryConfiguration> findByCountryCodeAndStatus(String countryCode, ConfigurationStatus status);

    Optional<CountryConfiguration> findFirstByCountryCodeAndConfigurationKeyOrderByVersionDesc(
            String countryCode, String configurationKey);
}