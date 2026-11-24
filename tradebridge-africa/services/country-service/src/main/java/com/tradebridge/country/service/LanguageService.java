package com.tradebridge.country.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.country.api.dto.request.AddLanguageRequest;
import com.tradebridge.country.api.dto.response.LanguageResponse;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.CountryLanguage;
import com.tradebridge.country.domain.Language;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.CountryLanguageRepository;
import com.tradebridge.country.repository.CountryRepository;
import com.tradebridge.country.repository.LanguageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Language catalogue and country-language associations (spec §4.1B, §4.4).
 */
@Service
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final CountryLanguageRepository countryLanguageRepository;
    private final CountryRepository countryRepository;
    private final CountryEventPublisher eventPublisher;

    public LanguageService(LanguageRepository languageRepository,
                           CountryLanguageRepository countryLanguageRepository,
                           CountryRepository countryRepository,
                           CountryEventPublisher eventPublisher) {
        this.languageRepository = languageRepository;
        this.countryLanguageRepository = countryLanguageRepository;
        this.countryRepository = countryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> listEnabled() {
        return languageRepository.findByEnabledTrueOrderByCodeAsc().stream()
                .map(l -> new LanguageResponse(l.getCode(), l.getName(), l.getNativeName(),
                        l.isEnabled(), false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> listForCountry(String countryCode) {
        String iso2 = requireCountry(countryCode).getIso2Code();
        return countryLanguageRepository.findByCountryCode(iso2).stream()
                .map(cl -> languageRepository.findByCode(cl.getLanguageCode())
                        .map(l -> new LanguageResponse(l.getCode(), l.getName(), l.getNativeName(),
                                l.isEnabled() && cl.isEnabled(), cl.isDefault()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public LanguageResponse addToCountry(String countryCode, AddLanguageRequest request, UUID actorId) {
        Country country = requireCountry(countryCode);
        String languageCode = request.languageCode().toLowerCase(Locale.ROOT);

        if (countryLanguageRepository.existsByCountryCodeAndLanguageCode(country.getIso2Code(), languageCode)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "Language already associated with country: " + languageCode);
        }

        Language language = languageRepository.findByCode(languageCode)
                .orElseGet(() -> languageRepository.save(new Language(
                        languageCode,
                        request.name() == null ? languageCode : request.name(),
                        request.nativeName(),
                        true)));

        if (request.isDefault()) {
            countryLanguageRepository.clearDefault(country.getIso2Code());
            country.updateDetails(null, null, null, null, languageCode.toUpperCase());
            countryRepository.save(country);
        }

        CountryLanguage link = new CountryLanguage(
                UuidV7Generator.next(), country.getIso2Code(), languageCode,
                request.isDefault(), true);
        countryLanguageRepository.save(link);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishLanguageUpdated(country.getIso2Code(), languageCode, correlationId);

        return new LanguageResponse(language.getCode(), language.getName(), language.getNativeName(),
                true, link.isDefault());
    }

    @Transactional
    public void removeFromCountry(String countryCode, String languageCode, UUID actorId) {
        Country country = requireCountry(countryCode);
        String code = languageCode.toLowerCase(Locale.ROOT);
        if (country.getDefaultLanguageCode().equalsIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot remove the default language; set another default first");
        }
        CountryLanguage link = countryLanguageRepository
                .findByCountryCodeAndLanguageCode(country.getIso2Code(), code)
                .orElseThrow(() -> ResourceNotFoundException.of("CountryLanguage", code));
        countryLanguageRepository.delete(link);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishLanguageUpdated(country.getIso2Code(), code, correlationId);
    }

    private Country requireCountry(String countryCode) {
        return countryRepository.findByIso2Code(countryCode.toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", countryCode));
    }
}