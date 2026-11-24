package com.tradebridge.country.cache;

import com.tradebridge.country.api.dto.response.ConfigurationResponse;
import com.tradebridge.country.api.dto.response.KycRequirementResponse;
import com.tradebridge.country.api.dto.response.OrganizationRequirementResponse;
import com.tradebridge.country.service.ConfigurationService;
import com.tradebridge.country.service.KycRequirementService;
import com.tradebridge.country.service.OrganizationRequirementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis-backed read cache for public country data.
 *
 * <p>Cache keys are `countryCode` alone (ISO2 uppercase). TTL and eviction
 * are managed by Spring Cache + Redis configuration in
 * {@link com.tradebridge.country.config.RedisCacheConfig}.
 *
 * <p>Eviction is triggered by
 * {@link com.tradebridge.country.cache.CacheEvictionListener} when a
 * `country.configuration.published.v1` event is emitted.
 *
 * <p>Never cache writes or authenticated state — this cache is strictly
 * for public read endpoints whose data changes only via admin actions.
 */
@Component
public class CountryConfigCache {

    private static final Logger log = LoggerFactory.getLogger(CountryConfigCache.class);

    public static final String CACHE_ACTIVE_CONFIG = "countryActiveConfig";
    public static final String CACHE_KYC_REQUIREMENTS = "countryKycRequirements";
    public static final String CACHE_ORG_REQUIREMENTS = "countryOrgRequirements";

    private final ConfigurationService configurationService;
    private final KycRequirementService kycRequirementService;
    private final OrganizationRequirementService organizationRequirementService;

    public CountryConfigCache(ConfigurationService configurationService,
                              KycRequirementService kycRequirementService,
                              OrganizationRequirementService organizationRequirementService) {
        this.configurationService = configurationService;
        this.kycRequirementService = kycRequirementService;
        this.organizationRequirementService = organizationRequirementService;
    }

    @Cacheable(cacheNames = CACHE_ACTIVE_CONFIG, key = "#countryCode.toUpperCase()")
    public ConfigurationResponse activeConfiguration(String countryCode) {
        log.debug("Cache miss — loading active configuration for {}", countryCode);
        return configurationService.getActive(countryCode);
    }

    @Cacheable(cacheNames = CACHE_KYC_REQUIREMENTS, key = "#countryCode.toUpperCase()")
    public List<KycRequirementResponse> kycRequirements(String countryCode) {
        log.debug("Cache miss — loading KYC requirements for {}", countryCode);
        return kycRequirementService.listActive(countryCode);
    }

    @Cacheable(cacheNames = CACHE_ORG_REQUIREMENTS, key = "#countryCode.toUpperCase()")
    public List<OrganizationRequirementResponse> organizationRequirements(String countryCode) {
        log.debug("Cache miss — loading organization requirements for {}", countryCode);
        return organizationRequirementService.listActive(countryCode);
    }

    /**
     * Manually evict all three caches for a country. Called from the
     * eviction listener and from any admin write path that wants to
     * proactively invalidate.
     */
    @CacheEvict(cacheNames = {
            CACHE_ACTIVE_CONFIG,
            CACHE_KYC_REQUIREMENTS,
            CACHE_ORG_REQUIREMENTS
    }, key = "#countryCode.toUpperCase()")
    public void evictCountry(String countryCode) {
        log.info("Evicted country cache entries for {}", countryCode);
    }
}