package com.tradebridge.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Notification-service specific configuration (spec §5).
 *
 * <pre>
 * tradebridge.notification:
 *   producer-service-name: notification-service
 *   internal-secret: "..."        # shared with identity/customer/others
 *   retry:
 *     max-attempts: 5
 *     initial-backoff: 30s
 *     max-backoff: 30m
 *     multiplier: 2.0
 *   bulk:
 *     max-batch-size: 500
 *   providers:
 *     email:
 *       enabled: true
 *       from-address: "noreply@tradebridge.africa"
 *       from-name: "TradeBridge Africa"
 *     sms:
 *       enabled: true
 *       primary: "africastalking"
 *       africastalking:
 *         username: "..."
 *         api-key: "..."
 *         sender-id: "TradeBridge"
 *       twilio:
 *         account-sid: "..."
 *         auth-token: "..."
 *         from-number: "+1..."
 *     inapp:
 *       enabled: true
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.notification")
public class NotificationProperties {

    private String producerServiceName = "notification-service";
    private String internalSecret;
    private final Retry retry = new Retry();
    private final Bulk bulk = new Bulk();
    private final Providers providers = new Providers();

    public String getProducerServiceName() { return producerServiceName; }
    public void setProducerServiceName(String v) { this.producerServiceName = v; }
    public String getInternalSecret() { return internalSecret; }
    public void setInternalSecret(String v) { this.internalSecret = v; }
    public Retry getRetry() { return retry; }
    public Bulk getBulk() { return bulk; }
    public Providers getProviders() { return providers; }

    public static class Retry {
        private int maxAttempts = 5;
        private Duration initialBackoff = Duration.ofSeconds(30);
        private Duration maxBackoff = Duration.ofMinutes(30);
        private double multiplier = 2.0;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public Duration getInitialBackoff() { return initialBackoff; }
        public void setInitialBackoff(Duration d) { this.initialBackoff = d; }
        public Duration getMaxBackoff() { return maxBackoff; }
        public void setMaxBackoff(Duration d) { this.maxBackoff = d; }
        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double v) { this.multiplier = v; }
    }

    public static class Bulk {
        private int maxBatchSize = 500;

        public int getMaxBatchSize() { return maxBatchSize; }
        public void setMaxBatchSize(int v) { this.maxBatchSize = v; }
    }

    public static class Providers {
        private final Email email = new Email();
        private final Sms sms = new Sms();
        private final InApp inApp = new InApp();

        public Email getEmail() { return email; }
        public Sms getSms() { return sms; }
        public InApp getInApp() { return inApp; }

        public static class Email {
            private boolean enabled = true;
            private String fromAddress = "noreply@tradebridge.africa";
            private String fromName = "TradeBridge Africa";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
            public String getFromAddress() { return fromAddress; }
            public void setFromAddress(String v) { this.fromAddress = v; }
            public String getFromName() { return fromName; }
            public void setFromName(String v) { this.fromName = v; }
        }

        public static class Sms {
            private boolean enabled = true;
            private String primary = "africastalking";
            private final AfricaTalking africaTalking = new AfricaTalking();
            private final Twilio twilio = new Twilio();

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
            public String getPrimary() { return primary; }
            public void setPrimary(String v) { this.primary = v; }
            public AfricaTalking getAfricaTalking() { return africaTalking; }
            public Twilio getTwilio() { return twilio; }

            public static class AfricaTalking {
                private String username;
                private String apiKey;
                private String senderId;

                public String getUsername() { return username; }
                public void setUsername(String v) { this.username = v; }
                public String getApiKey() { return apiKey; }
                public void setApiKey(String v) { this.apiKey = v; }
                public String getSenderId() { return senderId; }
                public void setSenderId(String v) { this.senderId = v; }
            }

            public static class Twilio {
                private String accountSid;
                private String authToken;
                private String fromNumber;

                public String getAccountSid() { return accountSid; }
                public void setAccountSid(String v) { this.accountSid = v; }
                public String getAuthToken() { return authToken; }
                public void setAuthToken(String v) { this.authToken = v; }
                public String getFromNumber() { return fromNumber; }
                public void setFromNumber(String v) { this.fromNumber = v; }
            }
        }

        public static class InApp {
            private boolean enabled = true;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
        }
    }
}
