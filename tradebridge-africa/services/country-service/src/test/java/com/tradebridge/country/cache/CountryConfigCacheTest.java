package com.tradebridge.country.cache;

import com.tradebridge.country.api.dto.response.ConfigurationResponse;
import com.tradebridge.country.api.dto.response.KycRequirementResponse;
import com.tradebridge.country.api.dto.response.OrganizationRequirementResponse;
import com.tradebridge.country.service.ConfigurationService;
import com.tradebridge.country.service.KycRequirementService;
import com.tradebridge.country.service.OrganizationRequirementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies caching semantics using a Spring context with an in-memory
 * cache manager (Redis serialization is covered by integration tests).
 */
@SpringJUnitConfig(CountryConfigCacheTest.TestConfig.class)
class CountryConfigCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CountryConfigCache.CACHE_ACTIVE_CONFIG,
                    CountryConfigCache.CACHE_KYC_REQUIREMENTS,
                    CountryConfigCache.CACHE_ORG_REQUIREMENTS);
        }

        @Bean
        public ConfigurationService configurationService() {
            return mock(ConfigurationService.class);
        }

        @Bean
        public KycRequirementService kycRequirementService() {
            return mock(KycRequirementService.class);
        }

        @Bean
        public OrganizationRequirementService organizationRequirementService() {
            return mock(OrganizationRequirementService.class);
        }

        @Bean
        public CountryConfigCache countryConfigCache(ConfigurationService cs,
                                                     KycRequirementService ks,
                                                     OrganizationRequirementService os) {
            return new CountryConfigCache(cs, ks, os);
        }
    }

    @Autowired private CountryConfigCache cache;
    @Autowired private ConfigurationService configurationService;
    @Autowired private KycRequirementService kycRequirementService;
    @Autowired private OrganizationRequirementService organizationRequirementService;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var c = cacheManager.getCache(name);
            if (c != null) c.clear();
        });
        clearInvocations(configurationService, kycRequirementService, organizationRequirementService);
    }

    @Test
    void activeConfiguration_isCachedPerCountry() {
        ConfigurationResponse za = new ConfigurationResponse("ZA", 1, "PUBLISHED",
                "initial", Map.of(), null, null);
        when(configurationService.getActive("ZA")).thenReturn(za);

        ConfigurationResponse first = cache.activeConfiguration("ZA");
        ConfigurationResponse second = cache.activeConfiguration("ZA");
        ConfigurationResponse third = cache.activeConfiguration("za"); // case-insensitive key

        assertThat(first).isSameAs(za);
        assertThat(second).isSameAs(za);
        assertThat(third).isSameAs(za);
        verify(configurationService, times(1)).getActive("ZA");
    }

    @Test
    void differentCountriesUseSeparateCacheEntries() {
        ConfigurationResponse za = new ConfigurationResponse("ZA", 1, "PUBLISHED",
                "initial", Map.of(), null, null);
        ConfigurationResponse pt = new ConfigurationResponse("PT", 1, "PUBLISHED",
                "initial", Map.of(), null, null);
        when(configurationService.getActive("ZA")).thenReturn(za);
        when(configurationService.getActive("PT")).thenReturn(pt);

        cache.activeConfiguration("ZA");
        cache.activeConfiguration("PT");

        verify(configurationService, times(1)).getActive("ZA");
        verify(configurationService, times(1)).getActive("PT");
    }

    @Test
    void evictCountry_removesCacheEntries() {
        ConfigurationResponse za = new ConfigurationResponse("ZA", 1, "PUBLISHED",
                "v1", Map.of(), null, null);
        ConfigurationResponse zaV2 = new ConfigurationResponse("ZA", 2, "PUBLISHED",
                "v2", Map.of(), null, null);
        when(configurationService.getActive("ZA"))
                .thenReturn(za)
                .thenReturn(zaV2);

        // Prime the cache
        assertThat(cache.activeConfiguration("ZA").version()).isEqualTo(1);

        // Evict
        cache.evictCountry("ZA");

        // Next call must re-invoke the service and return v2
        assertThat(cache.activeConfiguration("ZA").version()).isEqualTo(2);
        verify(configurationService, times(2)).getActive("ZA");
    }

    @Test
    void kycRequirements_cachedSeparately() {
        KycRequirementResponse req = new KycRequirementResponse(
                java.util.UUID.randomUUID(), "ZA", "COMPANY", "ID", "ID_COPY",
                "desc", true, List.of("application/pdf"), 1_000_000L, 365, true);
        when(kycRequirementService.listActive("ZA")).thenReturn(List.of(req));

        cache.kycRequirements("ZA");
        cache.kycRequirements("ZA");

        verify(kycRequirementService, times(1)).listActive("ZA");
    }

    @Test
    void organizationRequirements_cachedSeparately() {
        OrganizationRequirementResponse req = new OrganizationRequirementResponse(
                java.util.UUID.randomUUID(), "ZA", "COMPANY", "REG", "desc",
                true, null, null, null, true);
        when(organizationRequirementService.listActive("ZA")).thenReturn(List.of(req));

        cache.organizationRequirements("ZA");
        cache.organizationRequirements("ZA");

        verify(organizationRequirementService, times(1)).listActive("ZA");
    }

    @Test
    void evictCountry_clearsAllThreeCaches() {
        ConfigurationResponse za = new ConfigurationResponse("ZA", 1, "PUBLISHED",
                "v1", Map.of(), null, null);
        KycRequirementResponse kyc = new KycRequirementResponse(
                java.util.UUID.randomUUID(), "ZA", "COMPANY", "ID", "ID_COPY",
                null, true, List.of(), null, null, true);
        OrganizationRequirementResponse org = new OrganizationRequirementResponse(
                java.util.UUID.randomUUID(), "ZA", "COMPANY", "REG", null,
                true, null, null, null, true);
        when(configurationService.getActive("ZA")).thenReturn(za);
        when(kycRequirementService.listActive("ZA")).thenReturn(List.of(kyc));
        when(organizationRequirementService.listActive("ZA")).thenReturn(List.of(org));

        cache.activeConfiguration("ZA");
        cache.kycRequirements("ZA");
        cache.organizationRequirements("ZA");

        cache.evictCountry("ZA");

        cache.activeConfiguration("ZA");
        cache.kycRequirements("ZA");
        cache.organizationRequirements("ZA");

        verify(configurationService, times(2)).getActive("ZA");
        verify(kycRequirementService, times(2)).listActive("ZA");
        verify(organizationRequirementService, times(2)).listActive("ZA");
    }
}