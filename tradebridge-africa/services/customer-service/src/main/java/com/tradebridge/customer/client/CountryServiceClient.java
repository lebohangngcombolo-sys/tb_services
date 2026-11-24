package com.tradebridge.customer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.customer.client.dto.ActiveConfigurationResponse;
import com.tradebridge.customer.client.dto.KycRequirementsResponse;
import com.tradebridge.customer.client.dto.OrganizationRequirementsResponse;
import com.tradebridge.customer.config.CustomerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Country Service client (spec §6.3).
 *
 * <p>Fetches:
 * <ul>
 *   <li>Active configuration + version (recorded on KYC case creation)</li>
 *   <li>KYC requirements per country</li>
 *   <li>Organization requirements per country</li>
 * </ul>
 *
 * <p>All endpoints are public on Country Service — no JWT required.
 * Failures fail closed with clear error codes.
 */
@Component
public class CountryServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CountryServiceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CountryServiceClient(RestClient.Builder builder,
                                 CustomerProperties properties,
                                 ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl(properties.getCountryService().getBaseUrl()).build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch the currently active configuration + version for a country.
     * Required at KYC case creation (spec §11 decision 3).
     */
    public ActiveConfigurationResponse getActiveConfiguration(String countryCode) {
        try {
            JsonNode response = restClient.get()
                    .uri("/api/v1/countries/{code}/configuration", countryCode.toUpperCase())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("success").asBoolean(false)) {
                throw new BusinessException(ErrorCode.DOWNSTREAM_SERVICE_UNAVAILABLE,
                        "Country configuration unavailable for " + countryCode);
            }
            JsonNode data = response.path("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.convertValue(
                    data.path("configuration"), Map.class);

            return new ActiveConfigurationResponse(
                    data.path("countryCode").asText(),
                    data.path("version").asInt(),
                    data.path("status").asText(),
                    data.path("changeSummary").asText(null),
                    config,
                    parseInstant(data.path("publishedAt").asText(null)),
                    parseInstant(data.path("createdAt").asText(null)));
        } catch (RestClientException e) {
            log.warn("Country Service unavailable fetching active configuration for {}: {}",
                    countryCode, e.getMessage());
            throw new BusinessException(ErrorCode.DOWNSTREAM_SERVICE_UNAVAILABLE,
                    "Country Service is currently unavailable");
        }
    }

    /**
     * Fetch the KYC requirements for a country, filtered by customer type
     * (COMPANY / INDIVIDUAL). Empty list if none configured.
     */
    public KycRequirementsResponse getKycRequirements(String countryCode, String customerType) {
        try {
            JsonNode response = restClient.get()
                    .uri("/api/v1/countries/{code}/kyc-requirements", countryCode.toUpperCase())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("success").asBoolean(false)) {
                return new KycRequirementsResponse(List.of());
            }
            JsonNode data = response.path("data");
            if (!data.isArray()) {
                return new KycRequirementsResponse(List.of());
            }
            List<KycRequirementsResponse.Item> items = new ArrayList<>();
            for (JsonNode item : data) {
                String reqCustomerType = item.path("customerType").asText("");
                if (customerType != null && !customerType.isBlank()
                        && !customerType.equalsIgnoreCase(reqCustomerType)) {
                    continue;
                }
                items.add(new KycRequirementsResponse.Item(
                        parseUuid(item.path("id").asText(null)),
                        item.path("countryCode").asText(),
                        reqCustomerType,
                        item.path("documentType").asText(),
                        item.path("requirementCode").asText(),
                        item.path("description").asText(null),
                        item.path("required").asBoolean(false),
                        toStringList(item.path("allowedMimeTypes")),
                        item.has("maxFileSizeBytes") && !item.path("maxFileSizeBytes").isNull()
                                ? item.path("maxFileSizeBytes").asLong() : null,
                        item.has("validityDays") && !item.path("validityDays").isNull()
                                ? item.path("validityDays").asInt() : null,
                        item.path("enabled").asBoolean(true)));
            }
            return new KycRequirementsResponse(items);
        } catch (RestClientException e) {
            log.warn("Country Service unavailable fetching KYC requirements for {}: {}",
                    countryCode, e.getMessage());
            return new KycRequirementsResponse(List.of());
        }
    }

    /**
     * Fetch the organization requirements for a country, filtered by org
     * type. Empty list if none configured.
     */
    public OrganizationRequirementsResponse getOrganizationRequirements(String countryCode,
                                                                         String organizationType) {
        try {
            JsonNode response = restClient.get()
                    .uri("/api/v1/countries/{code}/organization-requirements", countryCode.toUpperCase())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("success").asBoolean(false)) {
                return new OrganizationRequirementsResponse(List.of());
            }
            JsonNode data = response.path("data");
            if (!data.isArray()) {
                return new OrganizationRequirementsResponse(List.of());
            }
            List<OrganizationRequirementsResponse.Item> items = new ArrayList<>();
            for (JsonNode item : data) {
                String reqOrgType = item.path("organizationType").asText("");
                if (organizationType != null && !organizationType.isBlank()
                        && !organizationType.equalsIgnoreCase(reqOrgType)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> validationRule = item.has("validationRule")
                        && !item.path("validationRule").isNull()
                        ? objectMapper.convertValue(item.path("validationRule"), Map.class)
                        : null;
                items.add(new OrganizationRequirementsResponse.Item(
                        parseUuid(item.path("id").asText(null)),
                        item.path("countryCode").asText(),
                        reqOrgType,
                        item.path("requirementCode").asText(),
                        item.path("description").asText(null),
                        item.path("required").asBoolean(false),
                        validationRule,
                        item.path("enabled").asBoolean(true)));
            }
            return new OrganizationRequirementsResponse(items);
        } catch (RestClientException e) {
            log.warn("Country Service unavailable fetching org requirements for {}: {}",
                    countryCode, e.getMessage());
            return new OrganizationRequirementsResponse(List.of());
        }
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> out = new ArrayList<>(node.size());
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    private static java.util.UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return java.util.UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static java.time.Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return java.time.Instant.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}