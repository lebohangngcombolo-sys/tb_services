package com.tradebridge.identity.service;

import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.common.response.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Validates that a country is enabled before registration completes
 * (spec §3.4 "Country code must exist and be enabled in Country Service").
 *
 * <p>Country Service is the authoritative source. This client is
 * defensive: if the country service is unreachable, we fail closed for
 * new registrations (spec §4.4 "A disabled country cannot be selected
 * for new onboarding").
 *
 * <p>The response is a public {@code GET /api/v1/countries/{code}}
 * endpoint, no JWT required.
 */
@Component
public class CountryValidationClient {

    private static final Logger log = LoggerFactory.getLogger(CountryValidationClient.class);

    private final RestClient restClient;
    private final Duration timeout;

    public CountryValidationClient(
            RestClient.Builder restClientBuilder,
            @Value("${tradebridge.identity.country-service.base-url:http://country-service:8082}") String baseUrl,
            @Value("${tradebridge.identity.country-service.timeout:3s}") Duration timeout) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.timeout = timeout;
    }

    /**
     * Ensures the country is enabled. Throws {@link ValidationException} if
     * the country does not exist, is disabled, or the service is unavailable.
     */
    public void requireEnabled(String countryCode) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/api/v1/countries/{code}", countryCode)
                    .retrieve()
                    .body(Map.class);

            if (response == null || Boolean.FALSE.equals(response.get("success"))) {
                throw validationError(countryCode, "Country could not be verified");
            }
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> country)) {
                throw validationError(countryCode, "Country not found");
            }
            Object enabled = country.get("enabled");
            if (!Boolean.TRUE.equals(enabled)) {
                throw validationError(countryCode, "Country is not enabled for onboarding");
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (RestClientException rce) {
            log.warn("Country Service unavailable during validation for '{}': {}",
                    countryCode, rce.getMessage());
            throw validationError(countryCode,
                    "Country configuration temporarily unavailable; please retry");
        }
    }

    private static ValidationException validationError(String countryCode, String message) {
        return new ValidationException(message,
                List.of(new FieldError("countryCode", message + " (" + countryCode + ")")));
    }
}
