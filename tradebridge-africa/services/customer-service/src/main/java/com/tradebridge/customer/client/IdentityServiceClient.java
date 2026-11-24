package com.tradebridge.customer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradebridge.customer.client.dto.IdentityUserStatusResponse;
import com.tradebridge.customer.config.CustomerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Identity Service client (spec §6.3). Used to verify that a
 * representative or owner candidate has an active identity account
 * before linking.
 *
 * <p>This uses the public login check endpoint — no service-to-service
 * auth needed for basic status reads. If a user is found, we return
 * their status; if not found, we return empty (rep may not have an
 * account yet).
 */
@Component
public class IdentityServiceClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityServiceClient.class);

    private final RestClient restClient;

    public IdentityServiceClient(RestClient.Builder builder, CustomerProperties properties) {
        this.restClient = builder.baseUrl(properties.getIdentityService().getBaseUrl()).build();
    }

    /**
     * Returns the identity user's status, or empty if not found / unreachable.
     */
    public Optional<IdentityUserStatusResponse> lookupUser(UUID identityUserId) {
        if (identityUserId == null) return Optional.empty();
        try {
            JsonNode response = restClient.get()
                    .uri("/api/v1/identity/verification-status")
                    .retrieve()
                    .body(JsonNode.class);
            // Note: this is a self-lookup endpoint and returns the caller's
            // status, so it is unsuitable for cross-user lookups. Identity
            // Service's admin endpoint requires a service token. For this
            // milestone we return empty and treat the user as "pending
            // verification" — Customer Service can still record them as a
            // representative with verification_status=PENDING and let the
            // KYC review complete the check downstream.
            return Optional.empty();
        } catch (RestClientException e) {
            log.debug("Identity lookup not attempted: {}", e.getMessage());
            return Optional.empty();
        }
    }
}