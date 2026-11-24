package com.tradebridge.notification.provider.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMS provider settings, shared by all SMS adapters (spec §5.5).
 */
@ConfigurationProperties(prefix = "tradebridge.notification.providers.sms")
public class SmsProviderProperties {

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