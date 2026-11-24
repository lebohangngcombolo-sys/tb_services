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
import java.util.Base64;
import java.util.Map;

/**
 * Twilio SMS provider (spec §5.5). Used as an alternate to Africa's Talking
 * via {@code tradebridge.notification.providers.sms.primary=twilio}.
 */
@Component
public class TwilioProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioProvider.class);
    public static final String NAME = "twilio";

    private final SmsProviderProperties properties;
    private final RestClient restClient;

    public TwilioProvider(SmsProviderProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl("https://api.twilio.com").build();
    }

    @Override public String name() { return NAME; }
    @Override public Channel channel() { return Channel.SMS; }

    @Override
    public boolean isEnabled() {
        var t = properties.getTwilio();
        return properties.isEnabled()
                && t.getAccountSid() != null && !t.getAccountSid().isBlank()
                && t.getAuthToken() != null && !t.getAuthToken().isBlank()
                && t.getFromNumber() != null && !t.getFromNumber().isBlank();
    }

    @Override
    public ProviderResult send(String destination, String subject, String body) {
        if (!isEnabled()) {
            return ProviderResult.failed(NAME, "PROVIDER_DISABLED", "Twilio not configured");
        }
        try {
            var t = properties.getTwilio();
            String credentials = Base64.getEncoder().encodeToString(
                    (t.getAccountSid() + ":" + t.getAuthToken()).getBytes(StandardCharsets.UTF_8));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", t.getAccountSid())
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body("From=" + urlEncode(t.getFromNumber())
                            + "&To=" + urlEncode(destination)
                            + "&Body=" + urlEncode(body))
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return ProviderResult.failed(NAME, "EMPTY_RESPONSE", "No response");
            }
            String sid = String.valueOf(response.getOrDefault("sid", ""));
            String status = String.valueOf(response.getOrDefault("status", ""));
            boolean ok = status != null
                    && (status.equals("queued") || status.equals("accepted")
                        || status.equals("sent") || status.equals("delivered"));
            if (!ok) {
                return ProviderResult.failed(NAME, "TWILIO_" + status.toUpperCase(), status);
            }
            log.info("Twilio SMS accepted for {}", maskPhone(destination));
            return ProviderResult.accepted(NAME, sid);
        } catch (RestClientException e) {
            log.warn("Twilio transport error: {}", e.getClass().getSimpleName());
            return ProviderResult.failed(NAME, "TWILIO_TRANSPORT", e.getMessage());
        } catch (Exception e) {
            log.warn("Twilio unexpected error: {}", e.getClass().getSimpleName());
            return ProviderResult.failed(NAME, "TWILIO_ERROR", e.getMessage());
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