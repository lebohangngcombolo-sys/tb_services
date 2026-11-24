package com.tradebridge.notification.provider.sms;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.notification.provider.NotificationProvider;
import com.tradebridge.notification.provider.ProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Africa's Talking SMS provider (spec §5.5).
 *
 * <p>Docs: https://developers.africastalking.com/docs/sms/sending
 * <p>Never logs the SMS body — OTPs are transmitted through this channel.
 */
@Component
public class AfricaTalkingProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(AfricaTalkingProvider.class);
    public static final String NAME = "africastalking";
    private static final String BASE_URL = "https://api.africastalking.com/version1/messaging";

    private final SmsProviderProperties properties;
    private final RestClient restClient;

    public AfricaTalkingProvider(SmsProviderProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override public String name() { return NAME; }
    @Override public Channel channel() { return Channel.SMS; }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled()
                && properties.getAfricaTalking().getUsername() != null
                && !properties.getAfricaTalking().getUsername().isBlank()
                && properties.getAfricaTalking().getApiKey() != null
                && !properties.getAfricaTalking().getApiKey().isBlank();
    }

    @Override
    public ProviderResult send(String destination, String subject, String body) {
        if (!isEnabled()) {
            return ProviderResult.failed(NAME, "PROVIDER_DISABLED", "Africa's Talking not configured");
        }
        try {
            var props = properties.getAfricaTalking();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .header("apiKey", props.getApiKey())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body("username=" + urlEncode(props.getUsername())
                            + "&to=" + urlEncode(destination)
                            + "&from=" + urlEncode(props.getSenderId())
                            + "&message=" + urlEncode(body))
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return ProviderResult.failed(NAME, "EMPTY_RESPONSE", "No response body");
            }

            // Standard AT response: { SMSMessageData: { Recipients: [ { status, messageId } ] } }
            Map<?, ?> data = (Map<?, ?>) response.get("SMSMessageData");
            if (data == null) {
                return ProviderResult.failed(NAME, "UNEXPECTED_RESPONSE", "Missing SMSMessageData");
            }
            Object recipientsObj = data.get("Recipients");
            if (!(recipientsObj instanceof java.util.List<?> recipients) || recipients.isEmpty()) {
                return ProviderResult.failed(NAME, "NO_RECIPIENT", "Provider returned no recipients");
            }
            Object first = recipients.get(0);
            if (!(first instanceof Map<?, ?> recipient)) {
                return ProviderResult.failed(NAME, "BAD_RECIPIENT", "Malformed recipient entry");
            }
            String status = String.valueOf(recipient.get("status"));
            String messageId = String.valueOf(recipient.get("messageId"));
            boolean ok = "Success".equalsIgnoreCase(status);
            if (!ok) {
                return ProviderResult.failed(NAME, "AT_" + status.toUpperCase(), status);
            }
            log.info("AT SMS accepted for {}", maskPhone(destination));
            return ProviderResult.accepted(NAME, messageId);
        } catch (RestClientException e) {
            log.warn("AT transport error: {}", e.getClass().getSimpleName());
            return ProviderResult.failed(NAME, "AT_TRANSPORT", e.getMessage());
        } catch (Exception e) {
            log.warn("AT unexpected error: {}", e.getClass().getSimpleName());
            return ProviderResult.failed(NAME, "AT_ERROR", e.getMessage());
        }
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 5) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}