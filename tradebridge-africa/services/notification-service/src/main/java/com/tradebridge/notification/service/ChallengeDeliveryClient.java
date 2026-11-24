
package com.tradebridge.notification.service;

import com.tradebridge.notification.config.NotificationProperties;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ChallengeDeliveryClient {

    private static final Logger log =
        LoggerFactory.getLogger(ChallengeDeliveryClient.class);

    private final RestClient restClient;
    private final NotificationProperties properties;

    public ChallengeDeliveryClient(
        RestClient.Builder builder,
        NotificationProperties properties,
        @Value("${tradebridge.notification.identity-service.base-url:http://identity-service:8081}")
        String baseUrl) {

        this.restClient = builder.baseUrl(baseUrl).build();
        this.properties = properties;
    }

    public Optional<DeliveryDetails> fetchChallenge(UUID challengeId) {

        try {
            Map<String, Object> response = restClient.get()
                .uri("/api/v1/identity/internal/challenge-delivery/{id}",
                    challengeId)
                .header("X-Internal-Secret",
                    properties.getInternalSecret())
                .header("X-Caller-Service",
                    "notification-service")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });

            if (response == null) {
                throw new IllegalStateException(
                    "Identity returned an empty challenge response");
            }

            Object data = response.get("data");

            if (!(data instanceof Map<?, ?> payload)) {
                throw new IllegalStateException(
                    "Identity returned an invalid challenge response");
            }

            Object code = payload.get("code");
            Object destination = payload.get("destination");

            if (code == null || code.toString().isBlank()) {
                throw new IllegalStateException(
                    "Identity challenge response is missing OTP");
            }

            String email = validateEmail(
                destination == null ? null : destination.toString()
            );

            log.info(
                "Challenge retrieved successfully: id={}, destinationPresent={}",
                challengeId,
                true
            );

            return Optional.of(
                new DeliveryDetails(code.toString(), email)
            );

        } catch (HttpClientErrorException.NotFound e) {

            log.warn(
                "Challenge {} was not found or is already consumed",
                challengeId
            );

            return Optional.empty();

        } catch (RestClientException e) {

            log.error(
                "Failed to fetch challenge {} from identity: {}",
                challengeId,
                e.getClass().getSimpleName()
            );

            throw new IllegalStateException(
                "Identity challenge fetch failed", e
            );
        }
    }

    private static String validateEmail(String destination) {

        if (destination == null || destination.isBlank()) {
            throw new IllegalStateException(
                "Identity returned an empty email destination");
        }

        String normalized = destination.trim();

        try {
            InternetAddress address =
                new InternetAddress(normalized, true);

            address.validate();

            if (!normalized.equals(address.getAddress())) {
                throw new IllegalStateException(
                    "Identity returned an unexpected email format");
            }

            return normalized;

        } catch (AddressException e) {

            throw new IllegalStateException(
                "Identity returned an invalid email destination", e
            );
        }
    }

    public record DeliveryDetails(
        String code,
        String destination
    ) { }
}
