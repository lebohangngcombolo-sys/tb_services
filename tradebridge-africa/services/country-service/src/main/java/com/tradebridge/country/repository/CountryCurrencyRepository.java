package com.tradebridge.country.repository;

import com.tradebridge.country.domain.CountryCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountryCurrencyRepository extends JpaRepository<CountryCurrency, UUID> {

    List<CountryCurrency> findByCountryCode(String countryCode);

    Optional<CountryCurrency> findByCountryCodeAndCurrencyCode(String countryCode, String currencyCode);

    List<CountryCurrency> findByCountryCodeAndEnabledTrue(String countryCode);

    Optional<CountryCurrency> findByCountryCodeAndIsDefaultTrue(String countryCode);

    boolean existsByCountryCodeAndCurrencyCode(String countryCode, String currencyCode);

    @Modifying
    @Query("UPDATE CountryCurrency cc SET cc.isDefault = FALSE "
            + "WHERE cc.countryCode = :countryCode")
    void clearDefault(@Param("countryCode") String countryCode);
}