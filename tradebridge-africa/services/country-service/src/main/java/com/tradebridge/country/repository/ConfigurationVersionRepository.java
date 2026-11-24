package com.tradebridge.country.repository;

import com.tradebridge.country.domain.ConfigurationVersion;
import com.tradebridge.country.domain.enums.ConfigurationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigurationVersionRepository extends JpaRepository<ConfigurationVersion, UUID> {

    List<ConfigurationVersion> findByCountryCodeOrderByVersionDesc(String countryCode);

    Optional<ConfigurationVersion> findByCountryCodeAndVersion(String countryCode, int version);

    Optional<ConfigurationVersion> findByCountryCodeAndStatus(String countryCode, ConfigurationStatus status);

    List<ConfigurationVersion> findByCountryCodeAndStatusOrderByVersionAsc(
            String countryCode, ConfigurationStatus status);

    @Query("SELECT MAX(cv.version) FROM ConfigurationVersion cv WHERE cv.countryCode = :countryCode")
    Optional<Integer> findMaxVersion(@Param("countryCode") String countryCode);
}