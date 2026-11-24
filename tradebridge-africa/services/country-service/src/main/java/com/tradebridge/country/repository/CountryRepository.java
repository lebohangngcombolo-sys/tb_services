package com.tradebridge.country.repository;

import com.tradebridge.country.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {

    Optional<Country> findByIso2Code(String iso2Code);

    Optional<Country> findByIso3Code(String iso3Code);

    boolean existsByIso2Code(String iso2Code);

    boolean existsByIso3Code(String iso3Code);

    List<Country> findByEnabledTrueOrderByNameAsc();
}