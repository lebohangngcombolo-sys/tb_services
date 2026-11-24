package com.tradebridge.country.service;

import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.country.api.dto.response.ConfigurationVersionResponse;
import com.tradebridge.country.domain.ConfigurationVersion;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.repository.ConfigurationVersionRepository;
import com.tradebridge.country.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only operations over configuration version headers (spec §4.1E).
 */
@Service
public class ConfigurationVersionService {

    private final ConfigurationVersionRepository versionRepository;
    private final CountryRepository countryRepository;

    public ConfigurationVersionService(ConfigurationVersionRepository versionRepository,
                                        CountryRepository countryRepository) {
        this.versionRepository = versionRepository;
        this.countryRepository = countryRepository;
    }

    @Transactional(readOnly = true)
    public List<ConfigurationVersionResponse> listVersions(String countryCode) {
        Country country = requireCountry(countryCode);
        return versionRepository.findByCountryCodeOrderByVersionDesc(country.getIso2Code()).stream()
                .map(ConfigurationVersionService::toResponse)
                .toList();
    }

    private Country requireCountry(String countryCode) {
        return countryRepository.findByIso2Code(countryCode.toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", countryCode));
    }

    private static ConfigurationVersionResponse toResponse(ConfigurationVersion cv) {
        return new ConfigurationVersionResponse(
                cv.getVersion(),
                cv.getStatus().name(),
                cv.getChangeSummary(),
                cv.getPublishedAt(),
                cv.getCreatedAt());
    }
}