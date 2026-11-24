package com.tradebridge.country.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.country.api.dto.request.AddCurrencyRequest;
import com.tradebridge.country.api.dto.response.CurrencyResponse;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.CountryCurrency;
import com.tradebridge.country.domain.Currency;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.CountryCurrencyRepository;
import com.tradebridge.country.repository.CountryRepository;
import com.tradebridge.country.repository.CurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Currency catalogue and country-currency associations (spec §4.1B, §4.4).
 *
 * <p>Default handling: exactly one currency per country is default. When
 * a new currency is added with {@code isDefault = true}, the previous
 * default is cleared in the same transaction.
 */
@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CountryCurrencyRepository countryCurrencyRepository;
    private final CountryRepository countryRepository;
    private final CountryEventPublisher eventPublisher;

    public CurrencyService(CurrencyRepository currencyRepository,
                           CountryCurrencyRepository countryCurrencyRepository,
                           CountryRepository countryRepository,
                           CountryEventPublisher eventPublisher) {
        this.currencyRepository = currencyRepository;
        this.countryCurrencyRepository = countryCurrencyRepository;
        this.countryRepository = countryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CurrencyResponse> listEnabled() {
        return currencyRepository.findByEnabledTrueOrderByCodeAsc().stream()
                .map(c -> new CurrencyResponse(c.getCode(), c.getName(), c.getSymbol(),
                        c.getDecimalPlaces(), c.isEnabled(), false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CurrencyResponse> listForCountry(String countryCode) {
        String iso2 = requireCountry(countryCode).getIso2Code();
        return countryCurrencyRepository.findByCountryCode(iso2).stream()
                .map(cc -> currencyRepository.findByCode(cc.getCurrencyCode())
                        .map(c -> new CurrencyResponse(c.getCode(), c.getName(), c.getSymbol(),
                                c.getDecimalPlaces(), c.isEnabled() && cc.isEnabled(), cc.isDefault()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public CurrencyResponse addToCountry(String countryCode, AddCurrencyRequest request, UUID actorId) {
        Country country = requireCountry(countryCode);
        String currencyCode = request.currencyCode().toUpperCase();

        if (countryCurrencyRepository.existsByCountryCodeAndCurrencyCode(country.getIso2Code(), currencyCode)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "Currency already associated with country: " + currencyCode);
        }

        // Get or create the currency catalogue entry
        Currency currency = currencyRepository.findByCode(currencyCode)
                .orElseGet(() -> currencyRepository.save(new Currency(
                        currencyCode,
                        request.name() == null ? currencyCode : request.name(),
                        request.symbol(),
                        (short) Math.max(0, request.decimalPlaces() == null ? 2 : request.decimalPlaces()),
                        true)));

        if (request.isDefault()) {
            countryCurrencyRepository.clearDefault(country.getIso2Code());
            // Update the country's default currency field
            country.updateDetails(null, null, null, currencyCode, null);
            countryRepository.save(country);
        }

        CountryCurrency link = new CountryCurrency(
                UuidV7Generator.next(), country.getIso2Code(), currencyCode,
                request.isDefault(), true);
        countryCurrencyRepository.save(link);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishCurrencyUpdated(country.getIso2Code(), currencyCode, correlationId);

        return new CurrencyResponse(currency.getCode(), currency.getName(), currency.getSymbol(),
                currency.getDecimalPlaces(), true, link.isDefault());
    }

    @Transactional
    public void removeFromCountry(String countryCode, String currencyCode, UUID actorId) {
        Country country = requireCountry(countryCode);
        String code = currencyCode.toUpperCase();

        if (country.getDefaultCurrencyCode().equals(code)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot remove the default currency; set another default first");
        }
        CountryCurrency link = countryCurrencyRepository
                .findByCountryCodeAndCurrencyCode(country.getIso2Code(), code)
                .orElseThrow(() -> ResourceNotFoundException.of("CountryCurrency", code));
        countryCurrencyRepository.delete(link);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishCurrencyUpdated(country.getIso2Code(), code, correlationId);
    }

    private Country requireCountry(String countryCode) {
        return countryRepository.findByIso2Code(countryCode.toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", countryCode));
    }
}