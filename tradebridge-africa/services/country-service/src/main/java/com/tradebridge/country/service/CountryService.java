package com.tradebridge.country.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.country.api.dto.request.CountryStatusRequest;
import com.tradebridge.country.api.dto.request.CreateCountryRequest;
import com.tradebridge.country.api.dto.request.UpdateCountryRequest;
import com.tradebridge.country.api.dto.response.CountryDetailResponse;
import com.tradebridge.country.api.dto.response.CountryResponse;
import com.tradebridge.country.api.dto.response.CurrencyResponse;
import com.tradebridge.country.api.dto.response.LanguageResponse;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.CountryCurrency;
import com.tradebridge.country.domain.CountryLanguage;
import com.tradebridge.country.domain.Currency;
import com.tradebridge.country.domain.Language;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.CountryCurrencyRepository;
import com.tradebridge.country.repository.CountryLanguageRepository;
import com.tradebridge.country.repository.CountryRepository;
import com.tradebridge.country.repository.CurrencyRepository;
import com.tradebridge.country.repository.LanguageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Country lifecycle (spec §4.1B, §4.4).
 *
 * <p>On create, we also create the default currency/language join rows
 * in the same transaction so that subsequent lookups do not see a
 * country without its defaults (spec §4.4 "A country must have an
 * enabled default currency/language").
 */
@Service
public class CountryService {

    private final CountryRepository countryRepository;
    private final CountryCurrencyRepository countryCurrencyRepository;
    private final CountryLanguageRepository countryLanguageRepository;
    private final CurrencyRepository currencyRepository;
    private final LanguageRepository languageRepository;
    private final CountryEventPublisher eventPublisher;

    public CountryService(CountryRepository countryRepository,
                          CountryCurrencyRepository countryCurrencyRepository,
                          CountryLanguageRepository countryLanguageRepository,
                          CurrencyRepository currencyRepository,
                          LanguageRepository languageRepository,
                          CountryEventPublisher eventPublisher) {
        this.countryRepository = countryRepository;
        this.countryCurrencyRepository = countryCurrencyRepository;
        this.countryLanguageRepository = countryLanguageRepository;
        this.currencyRepository = currencyRepository;
        this.languageRepository = languageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> listEnabled() {
        return countryRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(CountryService::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CountryDetailResponse getDetail(String countryCode) {
        Country country = requireCountry(countryCode);
        return toDetail(country);
    }

    @Transactional
    public CountryDetailResponse create(CreateCountryRequest request, UUID actorId) {
        String iso2 = request.iso2Code().toUpperCase();
        String iso3 = request.iso3Code().toUpperCase();
        if (countryRepository.existsByIso2Code(iso2)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Country ISO2 already exists: " + iso2);
        }
        if (countryRepository.existsByIso3Code(iso3)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Country ISO3 already exists: " + iso3);
        }

        Currency currency = currencyRepository.findByCode(request.defaultCurrencyCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Default currency not recognised: " + request.defaultCurrencyCode()));
        if (!currency.isEnabled()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Default currency is not enabled");
        }
        Language language = languageRepository.findByCode(request.defaultLanguageCode().toLowerCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Default language not recognised: " + request.defaultLanguageCode()));
        if (!language.isEnabled()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Default language is not enabled");
        }

        Country country = new Country(
                UuidV7Generator.next(),
                iso2, iso3,
                request.name(),
                request.region(),
                request.callingCode(),
                request.defaultCurrencyCode(),
                request.defaultLanguageCode(),
                request.enabled());
        countryRepository.save(country);

        // Create default currency + language join rows
        countryCurrencyRepository.save(new CountryCurrency(
                UuidV7Generator.next(), country.getIso2Code(), currency.getCode(), true, true));
        countryLanguageRepository.save(new CountryLanguage(
                UuidV7Generator.next(), country.getIso2Code(), language.getCode(), true, true));

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishCountryCreated(country.getIso2Code(), country.getName(), correlationId);

        return toDetail(country);
    }

    @Transactional
    public CountryDetailResponse update(String countryCode, UpdateCountryRequest request, UUID actorId) {
        Country country = requireCountry(countryCode);

        if (request.defaultCurrencyCode() != null) {
            Currency currency = currencyRepository.findByCode(request.defaultCurrencyCode())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Default currency not recognised: " + request.defaultCurrencyCode()));
            if (!currency.isEnabled()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Default currency is not enabled: " + currency.getCode());
            }
        }
        if (request.defaultLanguageCode() != null) {
            Language language = languageRepository.findByCode(request.defaultLanguageCode().toLowerCase())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Default language not recognised: " + request.defaultLanguageCode()));
            if (!language.isEnabled()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Default language is not enabled: " + language.getCode());
            }
        }

        country.updateDetails(request.name(), request.region(), request.callingCode(),
                request.defaultCurrencyCode(), request.defaultLanguageCode());
        countryRepository.save(country);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishCountryUpdated(country.getIso2Code(),
                String.join(",", describeChangedFields(request)), correlationId);

        return toDetail(country);
    }

    @Transactional
    public CountryDetailResponse changeStatus(String countryCode, CountryStatusRequest request, UUID actorId) {
        Country country = requireCountry(countryCode);
        boolean wasEnabled = country.isEnabled();
        if (request.enabled() && !wasEnabled) {
            country.enable();
        } else if (!request.enabled() && wasEnabled) {
            country.disable();
        } else {
            return toDetail(country);
        }
        countryRepository.save(country);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishCountryStatusChanged(
                country.getIso2Code(), country.isEnabled(), correlationId);

        return toDetail(country);
    }

    // ------------------------------------------------------------------ helpers

    Country requireCountry(String countryCode) {
        if (countryCode == null) {
            throw ResourceNotFoundException.of("Country", "null");
        }
        return countryRepository.findByIso2Code(countryCode.toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", countryCode));
    }

    private CountryDetailResponse toDetail(Country country) {
        List<CurrencyResponse> currencies = countryCurrencyRepository
                .findByCountryCode(country.getIso2Code())
                .stream()
                .map(cc -> currencyRepository.findByCode(cc.getCurrencyCode())
                        .map(c -> new CurrencyResponse(c.getCode(), c.getName(), c.getSymbol(),
                                c.getDecimalPlaces(), c.isEnabled() && cc.isEnabled(), cc.isDefault()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<LanguageResponse> languages = countryLanguageRepository
                .findByCountryCode(country.getIso2Code())
                .stream()
                .map(cl -> languageRepository.findByCode(cl.getLanguageCode())
                        .map(l -> new LanguageResponse(l.getCode(), l.getName(), l.getNativeName(),
                                l.isEnabled() && cl.isEnabled(), cl.isDefault()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        return new CountryDetailResponse(
                country.getIso2Code(), country.getIso3Code(), country.getName(),
                country.getRegion(), country.getCallingCode(),
                country.getDefaultCurrencyCode(), country.getDefaultLanguageCode(),
                country.isEnabled(), currencies, languages,
                country.getCreatedAt(), country.getUpdatedAt());
    }

    private static CountryResponse toSummary(Country c) {
        return new CountryResponse(c.getIso2Code(), c.getIso3Code(), c.getName(),
                c.getRegion(), c.getCallingCode(),
                c.getDefaultCurrencyCode(), c.getDefaultLanguageCode(), c.isEnabled());
    }

    private static List<String> describeChangedFields(UpdateCountryRequest r) {
        java.util.List<String> changed = new java.util.ArrayList<>();
        if (r.name() != null) changed.add("name");
        if (r.region() != null) changed.add("region");
        if (r.callingCode() != null) changed.add("callingCode");
        if (r.defaultCurrencyCode() != null) changed.add("defaultCurrencyCode");
        if (r.defaultLanguageCode() != null) changed.add("defaultLanguageCode");
        return changed;
    }
}