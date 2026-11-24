package com.tradebridge.country.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.country.api.dto.request.PublishConfigurationRequest;
import com.tradebridge.country.api.dto.request.RollbackConfigurationRequest;
import com.tradebridge.country.api.dto.response.ConfigurationResponse;
import com.tradebridge.country.domain.ConfigurationVersion;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.CountryConfiguration;
import com.tradebridge.country.domain.enums.ConfigurationStatus;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.ConfigurationVersionRepository;
import com.tradebridge.country.repository.CountryConfigurationRepository;
import com.tradebridge.country.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationServiceTest {

    @Mock private CountryRepository countryRepository;
    @Mock private ConfigurationVersionRepository versionRepository;
    @Mock private CountryConfigurationRepository configurationRepository;
    @Mock private CountryEventPublisher eventPublisher;

    private ConfigurationService service;
    private Country country;
    private ConfigurationVersion publishedV1;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new ConfigurationService(countryRepository, versionRepository,
                configurationRepository, eventPublisher);

        country = new Country(UUID.randomUUID(), "ZA", "ZAF", "South Africa",
                "AFRICA", "+27", "ZAR", "EN", true);
        publishedV1 = new ConfigurationVersion(UUID.randomUUID(), "ZA", 1,
                ConfigurationStatus.DRAFT, "Initial");
        publishedV1.publish(UUID.randomUUID(), "Initial");

        when(countryRepository.findByIso2Code("ZA")).thenReturn(Optional.of(country));
        when(countryRepository.findByIso2Code("XX")).thenReturn(Optional.empty());

        // Persist mock behavior: save() returns the argument; findMaxVersion returns current max
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publish_createsNewVersionAndSupersedesPrevious() {
        when(versionRepository.findMaxVersion("ZA")).thenReturn(Optional.of(1));
        when(versionRepository.findByCountryCodeAndStatus("ZA", ConfigurationStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedV1));
        when(configurationRepository.findByCountryCodeAndVersion(eq("ZA"), anyInt()))
                .thenReturn(List.of());

        ConfigurationResponse response = service.publish("ZA",
                new PublishConfigurationRequest("v2", Map.of(
                        "onboarding.min_directors", Map.of("value", 2))),
                UUID.randomUUID());

        assertThat(response.version()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.changeSummary()).isEqualTo("v2");
        assertThat(publishedV1.getStatus()).isEqualTo(ConfigurationStatus.SUPERSEDED);
        verify(versionRepository, times(2)).save(any(ConfigurationVersion.class));
        verify(eventPublisher).publishConfigurationPublished(eq("ZA"), eq(2), any());
    }

    @Test
    void publish_copiesPreviousVersionWhenNoBody() {
        when(versionRepository.findMaxVersion("ZA")).thenReturn(Optional.of(1));
        when(versionRepository.findByCountryCodeAndStatus("ZA", ConfigurationStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedV1));
        CountryConfiguration existingRow = new CountryConfiguration(
                UUID.randomUUID(), "ZA", "onboarding.min_directors",
                Map.of("value", 1), 1, ConfigurationStatus.PUBLISHED,
                null, null, UUID.randomUUID());
        when(configurationRepository.findByCountryCodeAndVersion("ZA", 1))
                .thenReturn(List.of(existingRow));
        when(configurationRepository.findByCountryCodeAndVersion("ZA", 2))
                .thenReturn(List.of());

        ConfigurationResponse response = service.publish("ZA",
                new PublishConfigurationRequest("carry forward", null),
                UUID.randomUUID());

        assertThat(response.version()).isEqualTo(2);
        verify(configurationRepository, times(1)).save(any(CountryConfiguration.class));
    }

    @Test
    void publish_rejectsUnknownCountry() {
        assertThatThrownBy(() -> service.publish("XX",
                new PublishConfigurationRequest("x", Map.of()), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rollback_restoresTargetVersion() {
        when(versionRepository.findMaxVersion("ZA")).thenReturn(Optional.of(2));
        ConfigurationVersion v1 = new ConfigurationVersion(UUID.randomUUID(), "ZA", 1,
                ConfigurationStatus.DRAFT, "v1");
        v1.publish(UUID.randomUUID(), "v1");
        v1.supersede();

        ConfigurationVersion v2 = new ConfigurationVersion(UUID.randomUUID(), "ZA", 2,
                ConfigurationStatus.DRAFT, "v2");
        v2.publish(UUID.randomUUID(), "v2");

        when(versionRepository.findByCountryCodeAndVersion("ZA", 1)).thenReturn(Optional.of(v1));
        when(versionRepository.findByCountryCodeAndVersion("ZA", 2)).thenReturn(Optional.of(v2));
        when(versionRepository.findByCountryCodeAndStatus("ZA", ConfigurationStatus.PUBLISHED))
                .thenReturn(Optional.of(v2));
        when(configurationRepository.findByCountryCodeAndVersion(eq("ZA"), anyInt()))
                .thenReturn(List.of());

        ConfigurationResponse response = service.rollback("ZA",
                new RollbackConfigurationRequest(1, "v2 broke things"), UUID.randomUUID());

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("ROLLED_BACK");
        assertThat(v2.getStatus()).isEqualTo(ConfigurationStatus.SUPERSEDED);
        assertThat(v1.getStatus()).isEqualTo(ConfigurationStatus.ROLLED_BACK);
        verify(eventPublisher).publishConfigurationPublished(eq("ZA"), eq(1), any());
    }

    @Test
    void rollback_rejectsDraft() {
        ConfigurationVersion draft = new ConfigurationVersion(UUID.randomUUID(), "ZA", 5,
                ConfigurationStatus.DRAFT, "draft");
        when(versionRepository.findByCountryCodeAndVersion("ZA", 5)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.rollback("ZA",
                new RollbackConfigurationRequest(5, "oops"), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void rollback_rejectsMissingTarget() {
        when(versionRepository.findByCountryCodeAndVersion("ZA", 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rollback("ZA",
                new RollbackConfigurationRequest(99, "nope"), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActive_noPublishedConfig_throws() {
        when(versionRepository.findByCountryCodeAndStatus("ZA", ConfigurationStatus.PUBLISHED))
                .thenReturn(Optional.empty());
        when(versionRepository.findByCountryCodeAndStatus("ZA", ConfigurationStatus.ROLLED_BACK))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActive("ZA"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}