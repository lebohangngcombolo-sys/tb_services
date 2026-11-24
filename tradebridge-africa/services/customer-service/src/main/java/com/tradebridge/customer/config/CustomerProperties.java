package com.tradebridge.customer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Customer-service specific configuration.
 *
 * <pre>
 * tradebridge.customer:
 *   producer-service-name: customer-service
 *   country-service:
 *     base-url: http://country-service:8082
 *     cache-ttl: 5m
 *   identity-service:
 *     base-url: http://identity-service:8081
 *   documents:
 *     storage: minio
 *     max-file-size-bytes: 52428800
 *     allowed-mime-types: [...]
 *     upload-url-expiry: 15m
 *   ownership:
 *     default-threshold: 25.0
 *   kyc:
 *     second-reviewer-risk-levels: [HIGH, CRITICAL]
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.customer")
public class CustomerProperties {

    private String producerServiceName = "customer-service";
    private final CountryService countryService = new CountryService();
    private final IdentityService identityService = new IdentityService();
    private final Documents documents = new Documents();
    private final Ownership ownership = new Ownership();
    private final Kyc kyc = new Kyc();

    public String getProducerServiceName() { return producerServiceName; }
    public void setProducerServiceName(String v) { this.producerServiceName = v; }
    public CountryService getCountryService() { return countryService; }
    public IdentityService getIdentityService() { return identityService; }
    public Documents getDocuments() { return documents; }
    public Ownership getOwnership() { return ownership; }
    public Kyc getKyc() { return kyc; }

    public static class CountryService {
        private String baseUrl = "http://country-service:8082";
        private Duration cacheTtl = Duration.ofMinutes(5);
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public Duration getCacheTtl() { return cacheTtl; }
        public void setCacheTtl(Duration d) { this.cacheTtl = d; }
    }

    public static class IdentityService {
        private String baseUrl = "http://identity-service:8081";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
    }

    public static class Documents {
        private String storage = "minio";
        private long maxFileSizeBytes = 52_428_800L;
        private java.util.List<String> allowedMimeTypes = java.util.List.of(
                "application/pdf", "image/jpeg", "image/png");
        private Duration uploadUrlExpiry = Duration.ofMinutes(15);
        public String getStorage() { return storage; }
        public void setStorage(String v) { this.storage = v; }
        public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
        public void setMaxFileSizeBytes(long v) { this.maxFileSizeBytes = v; }
        public java.util.List<String> getAllowedMimeTypes() { return allowedMimeTypes; }
        public void setAllowedMimeTypes(java.util.List<String> v) { this.allowedMimeTypes = v; }
        public Duration getUploadUrlExpiry() { return uploadUrlExpiry; }
        public void setUploadUrlExpiry(Duration d) { this.uploadUrlExpiry = d; }
    }

    public static class Ownership {
        private double defaultThreshold = 25.0;
        public double getDefaultThreshold() { return defaultThreshold; }
        public void setDefaultThreshold(double v) { this.defaultThreshold = v; }
    }

    public static class Kyc {
        private java.util.List<String> secondReviewerRiskLevels = java.util.List.of("HIGH", "CRITICAL");
        public java.util.List<String> getSecondReviewerRiskLevels() { return secondReviewerRiskLevels; }
        public void setSecondReviewerRiskLevels(java.util.List<String> v) { this.secondReviewerRiskLevels = v; }
    }
}
