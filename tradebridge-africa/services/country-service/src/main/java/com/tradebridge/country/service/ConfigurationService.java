package com.tradebridge.country.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.country.api.dto.request.PublishConfigurationRequest;
import com.tradebridge.country.api.dto.request.RollbackConfigurationRequest;
import com.tradebridge.country.api.dto.response.ConfigurationResponse;
import com.tradebridge.country.api.dto.response.ConfigurationVersionResponse;
import com.tradebridge.country.domain.ConfigurationVersion;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.CountryConfiguration;
import com.tradebridge.country.domain.enums.ConfigurationStatus;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.ConfigurationVersionRepository;
import com.tradebridge.country.repository.CountryConfigurationRepository;
import com.tradebridge.country.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration versioning (spec §4.1E, §4.4).
 *
 * <p>Atomic publish: version header + all config rows are created/superseded
 * in a single transaction. If anything fails, nothing changes.
 *
 * <p>Immutability: once a version leaves DRAFT, its rows are never modified.
 * Corrections require a new version.
 */
@Service
public class ConfigurationService {

    private final CountryRepository countryRepository;
    private final ConfigurationVersionRepository versionRepository;
    private final CountryConfigurationRepository configurationRepository;
    private final CountryEventPublisher eventPublisher;

    public ConfigurationService(CountryRepository countryRepository,
                                ConfigurationVersionRepository versionRepository,
                                CountryConfigurationRepository configurationRepository,
                                CountryEventPublisher eventPublisher) {
        this.countryRepository = countryRepository;
        this.versionRepository = versionRepository;
        this.configurationRepository = configurationRepository;
        this.eventPublisher = eventPublisher;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public ConfigurationResponse getActive(String countryCode) {
        Country country = requireCountry(countryCode);
        ConfigurationVersion published = versionRepository
                .findByCountryCodeAndStatus(country.getIso2Code(), ConfigurationStatus.PUBLISHED)
                .or(() -> versionRepository
                        .findByCountryCodeAndStatus(country.getIso2Code(), ConfigurationStatus.ROLLED_BACK))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "No published configuration for country " + countryCode));
        return buildResponse(published);
    }

    @Transactional(readOnly = true)
    public ConfigurationResponse getVersion(String countryCode, int version) {
        Country country = requireCountry(countryCode);
        ConfigurationVersion cv = versionRepository
                .findByCountryCodeAndVersion(country.getIso2Code(), version)
                .orElseThrow(() -> ResourceNotFoundException.of("ConfigurationVersion",
                        countryCode + "/v" + version));
        return buildResponse(cv);
    }

    // ------------------------------------------------------------------ write

    /**
     * Publishes a new configuration version atomically.
     *
     * <p>Steps:
     * <ol>
     *   <li>Find next version number = max + 1.</li>
     *   <li>Create a DRAFT version header.</li>
     *   <li>Insert config rows for the new version.</li>
     *   <li>Supersede the previous PUBLISHED version.</li>
     *   <li>Mark the new version as PUBLISHED.</li>
     * </ol>
     */
    @Transactional
    public ConfigurationResponse publish(String countryCode, PublishConfigurationRequest request, UUID actorId) {
        Country country = requireCountry(countryCode);
        int nextVersion = versionRepository.findMaxVersion(country.getIso2Code()).orElse(0) + 1;

        // 1. Create DRAFT header
        ConfigurationVersion newVersion = new ConfigurationVersion(
                UuidV7Generator.next(), country.getIso2Code(), nextVersion,
                ConfigurationStatus.DRAFT, request.changeSummary());
        versionRepository.save(newVersion);

        // 2. Insert config rows (from request body, or from previous version if empty)
        Map<String, Object> payload = request.configuration();
        if (payload == null || payload.isEmpty()) {
            // Copy the currently published configuration, if any
            versionRepository.findByCountryCodeAndStatus(country.getIso2Code(), ConfigurationStatus.PUBLISHED)
                    .ifPresent(prev -> {
                        List<CountryConfiguration> prevRows = configurationRepository
                                .findByCountryCodeAndVersion(country.getIso2Code(), prev.getVersion());
                        for (CountryConfiguration row : prevRows) {
                            configurationRepository.save(new CountryConfiguration(
                                    UuidV7Generator.next(),
                                    row.getCountryCode(),
                                    row.getConfigurationKey(),
                                    row.getConfigurationValue(),
                                    nextVersion,
                                    ConfigurationStatus.DRAFT,
                                    row.getEffectiveFrom(),
                                    row.getEffectiveUntil(),
                                    actorId));
                        }
                    });
        } else {
            for (Map.Entry<String, Object> e : payload.entrySet()) {
                Map<String, Object> valueMap = toValueMap(e.getValue());
                configurationRepository.save(new CountryConfiguration(
                        UuidV7Generator.next(),
                        country.getIso2Code(),
                        e.getKey(),
                        valueMap,
                        nextVersion,
                        ConfigurationStatus.DRAFT,
                        null, null, actorId));
            }
        }

        // 3. Supersede previous published version (if any)
        versionRepository.findByCountryCodeAndStatus(country.getIso2Code(), ConfigurationStatus.PUBLISHED)
                .ifPresent(ConfigurationVersion::supersede);

        // 4. Publish the new version
        newVersion.publish(actorId, request.changeSummary());
        versionRepository.save(newVersion);

        // 5. Update config rows from DRAFT to PUBLISHED to reflect their new status
        List<CountryConfiguration> newRows = configurationRepository
                .findByCountryCodeAndVersion(country.getIso2Code(), nextVersion);
        for (CountryConfiguration row : newRows) {
            configurationRepository.save(new CountryConfiguration(
                    UuidV7Generator.next(),
                    row.getCountryCode(),
                    row.getConfigurationKey(),
                    row.getConfigurationValue(),
                    nextVersion,
                    ConfigurationStatus.PUBLISHED,
                    row.getEffectiveFrom(),
                    row.getEffectiveUntil(),
                    row.getCreatedBy()));
        }

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishConfigurationPublished(country.getIso2Code(), nextVersion, correlationId);

        return buildResponse(newVersion);
    }

    @Transactional
    public ConfigurationResponse rollback(String countryCode, RollbackConfigurationRequest request, UUID actorId) {
        Country country = requireCountry(countryCode);
        ConfigurationVersion target = versionRepository
                .findByCountryCodeAndVersion(country.getIso2Code(), request.targetVersion())
                .orElseThrow(() -> ResourceNotFoundException.of("ConfigurationVersion",
                        countryCode + "/v" + request.targetVersion()));

        if (target.getStatus() == ConfigurationStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Cannot rollback to a DRAFT version");
        }

        // Mark current PUBLISHED as SUPERSEDED
        versionRepository.findByCountryCodeAndStatus(country.getIso2Code(), ConfigurationStatus.PUBLISHED)
                .ifPresent(ConfigurationVersion::supersede);

        // Restore target
        target.markRolledBack();
        versionRepository.save(target);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishConfigurationPublished(country.getIso2Code(), target.getVersion(), correlationId);

        return buildResponse(target);
    }

    // ------------------------------------------------------------------ helpers

    private ConfigurationResponse buildResponse(ConfigurationVersion version) {
        List<CountryConfiguration> rows = configurationRepository
                .findByCountryCodeAndVersion(version.getCountryCode(), version.getVersion());
        Map<String, Object> config = new HashMap<>();
        for (CountryConfiguration row : rows) {
            config.put(row.getConfigurationKey(), row.getConfigurationValue());
        }
        return new ConfigurationResponse(
                version.getCountryCode(),
                version.getVersion(),
                version.getStatus().name(),
                version.getChangeSummary(),
                config,
                version.getPublishedAt(),
                version.getCreatedAt());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toValueMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of("value", raw);
    }

    private Country requireCountry(String countryCode) {
        return countryRepository.findByIso2Code(countryCode.toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", countryCode));
    }
}