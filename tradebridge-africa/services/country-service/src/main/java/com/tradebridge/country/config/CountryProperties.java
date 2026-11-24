package com.tradebridge.country.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Country-service specific configuration.
 *
 * <pre>
 * tradebridge.country:
 *   producer-service-name: country-service
 *   cache:
 *     ttl: 5m
 *   config-publish:
 *     require-prior-draft: true
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.country")
public class CountryProperties {

    private String producerServiceName = "country-service";
    private final Cache cache = new Cache();
    private final ConfigPublish configPublish = new ConfigPublish();

    public String getProducerServiceName() { return producerServiceName; }
    public void setProducerServiceName(String v) { this.producerServiceName = v; }
    public Cache getCache() { return cache; }
    public ConfigPublish getConfigPublish() { return configPublish; }

    public static class Cache {
        private Duration ttl = Duration.ofMinutes(5);
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration d) { this.ttl = d; }
    }

    public static class ConfigPublish {
        private boolean requirePriorDraft = true;
        public boolean isRequirePriorDraft() { return requirePriorDraft; }
        public void setRequirePriorDraft(boolean v) { this.requirePriorDraft = v; }
    }
}
