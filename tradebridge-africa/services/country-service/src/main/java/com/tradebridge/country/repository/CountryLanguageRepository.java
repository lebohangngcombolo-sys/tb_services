package com.tradebridge.country.repository;

import com.tradebridge.country.domain.CountryLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountryLanguageRepository extends JpaRepository<CountryLanguage, UUID> {

    List<CountryLanguage> findByCountryCode(String countryCode);

    Optional<CountryLanguage> findByCountryCodeAndLanguageCode(String countryCode, String languageCode);

    List<CountryLanguage> findByCountryCodeAndEnabledTrue(String countryCode);

    Optional<CountryLanguage> findByCountryCodeAndIsDefaultTrue(String countryCode);

    boolean existsByCountryCodeAndLanguageCode(String countryCode, String languageCode);

    @Modifying
    @Query("UPDATE CountryLanguage cl SET cl.isDefault = FALSE "
            + "WHERE cl.countryCode = :countryCode")
    void clearDefault(@Param("countryCode") String countryCode);
}