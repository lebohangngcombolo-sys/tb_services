package com.tradebridge.country.service;

import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.country.api.dto.request.KycRequirementRequest;
import com.tradebridge.country.api.dto.response.KycRequirementResponse;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.KycRequirement;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.CountryRepository;
import com.tradebridge.country.repository.KycRequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KycRequirementServiceTest {

    @Mock private KycRequirementRepository repository;
    @Mock private CountryRepository countryRepository;
    @Mock private CountryEventPublisher eventPublisher;

    private KycRequirementService service;
    private Country country;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new KycRequirementService(repository, countryRepository, eventPublisher);

        country = new Country(UUID.randomUUID(), "ZA", "ZAF", "South Africa",
                "AFRICA", "+27", "ZAR", "EN", true);
        when(countryRepository.findByIso2Code("ZA")).thenReturn(Optional.of(country));
        when(countryRepository.findByIso2Code("XX")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_appliesDefaultsForMimeAndSize() {
        when(repository.existsByCountryCodeAndCustomerTypeAndRequirementCode(
                "ZA", "COMPANY", "DIRECTOR_ID_COPY")).thenReturn(false);

        KycRequirementResponse response = service.create("ZA",
                new KycRequirementRequest("COMPANY", "DIRECTOR_ID",
                        "DIRECTOR_ID_COPY", "Certified ID copy", true,
                        null, null, 365, true),
                UUID.randomUUID());

        assertThat(response.allowedMimeTypes())
                .containsExactlyInAnyOrder("application/pdf", "image/jpeg", "image/png");
        assertThat(response.maxFileSizeBytes()).isEqualTo(10L * 1024 * 1024);
        assertThat(response.validityDays()).isEqualTo(365);
        verify(eventPublisher).publishKycRequirementUpdated(eq("ZA"), eq("DIRECTOR_ID_COPY"), any());
    }

    @Test
    void create_honorsExplicitMimeAndSize() {
        when(repository.existsByCountryCodeAndCustomerTypeAndRequirementCode(anyString(),
                anyString(), anyString())).thenReturn(false);

        KycRequirementResponse response = service.create("ZA",
                new KycRequirementRequest("COMPANY", "PASSPORT", "PASSPORT_COPY",
                        "Passport", true,
                        List.of("application/pdf"), 1L * 1024 * 1024, 3650, true),
                UUID.randomUUID());

        assertThat(response.allowedMimeTypes()).containsExactly("application/pdf");
        assertThat(response.maxFileSizeBytes()).isEqualTo(1L * 1024 * 1024);
    }

    @Test
    void create_rejectsDuplicateRequirementCode() {
        when(repository.existsByCountryCodeAndCustomerTypeAndRequirementCode(
                "ZA", "COMPANY", "DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.create("ZA",
                new KycRequirementRequest("COMPANY", "X", "DUP",
                        null, true, null, null, null, true),
                UUID.randomUUID()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsNonPositiveSize() {
        assertThatThrownBy(() -> service.create("ZA",
                new KycRequirementRequest("COMPANY", "X", "X",
                        null, true, null, -1L, null, true),
                UUID.randomUUID()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsOversizedFile() {
        long tooBig = 100L * 1024 * 1024; // 100 MiB > 50 MiB cap
        assertThatThrownBy(() -> service.create("ZA",
                new KycRequirementRequest("COMPANY", "X", "X",
                        null, true, null, tooBig, null, true),
                UUID.randomUUID()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsNonPositiveValidity() {
        assertThatThrownBy(() -> service.create("ZA",
                new KycRequirementRequest("COMPANY", "X", "X",
                        null, true, null, null, 0, true),
                UUID.randomUUID()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsUnknownCountry() {
        assertThatThrownBy(() -> service.create("XX",
                new KycRequirementRequest("COMPANY", "X", "X",
                        null, true, null, null, null, true),
                UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_modifiesFieldsAndAuditsEvent() {
        KycRequirement existing = new KycRequirement(UUID.randomUUID(), "ZA",
                "COMPANY", "ID", "ID_COPY", "old", true, null, null, null, true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        KycRequirementResponse response = service.update(existing.getId(),
                new KycRequirementRequest("COMPANY", "ID", "ID_COPY",
                        "new desc", true, List.of("application/pdf"), 500_000L, 90, true),
                UUID.randomUUID());

        assertThat(response.description()).isEqualTo("new desc");
        assertThat(response.maxFileSizeBytes()).isEqualTo(500_000L);
        verify(eventPublisher).publishKycRequirementUpdated(eq("ZA"), eq("ID_COPY"), any());
    }

    @Test
    void changeStatus_togglesEnabledFlag() {
        KycRequirement existing = new KycRequirement(UUID.randomUUID(), "ZA",
                "COMPANY", "ID", "ID_COPY", null, true, null, null, null, true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        KycRequirementResponse response = service.changeStatus(existing.getId(), false, UUID.randomUUID());
        assertThat(response.enabled()).isFalse();

        response = service.changeStatus(existing.getId(), true, UUID.randomUUID());
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void listActive_returnsEnabledOnly() {
        KycRequirement enabled = new KycRequirement(UUID.randomUUID(), "ZA",
                "COMPANY", "ID", "ID_COPY", null, true, null, null, null, true);
        when(repository.findByCountryCodeAndEnabledTrue("ZA")).thenReturn(List.of(enabled));

        List<KycRequirementResponse> list = service.listActive("ZA");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).requirementCode()).isEqualTo("ID_COPY");
    }
}