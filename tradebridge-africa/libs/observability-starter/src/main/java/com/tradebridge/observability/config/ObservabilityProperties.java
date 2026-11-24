package com.tradebridge.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for the observability starter.
 *
 * <pre>
 * tradebridge.observability:
 *   service-name: identity-service
 *   environment: local
 *   region: af-south-1
 *   correlation:
 *     enabled: true
 *     header: X-Correlation-Id
 *   logging:
 *     json: true
 *     mask-sensitive: true
 *   metrics:
 *     enabled: true
 *   tracing:
 *     enabled: true
 *     sampling-probability: 0.1
 *   health:
 *     kafka:
 *       enabled: true
 *       timeout: 3s
 *     database:
 *       enabled: true
 *       validation-query: SELECT 1
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.observability")
public class ObservabilityProperties {

    private String serviceName = "unknown-service";
    private String environment;
    private String region;
    private final Correlation correlation = new Correlation();
    private final Logging logging = new Logging();
    private final Metrics metrics = new Metrics();
    private final Tracing tracing = new Tracing();
    private final Health health = new Health();

    public String getServiceName() { return serviceName; }
    public void setServiceName(String v) { this.serviceName = v; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String v) { this.environment = v; }
    public String getRegion() { return region; }
    public void setRegion(String v) { this.region = v; }
    public Correlation getCorrelation() { return correlation; }
    public Logging getLogging() { return logging; }
    public Metrics getMetrics() { return metrics; }
    public Tracing getTracing() { return tracing; }
    public Health getHealth() { return health; }

    public static class Correlation {
        private boolean enabled = true;
        private String header = "X-Correlation-Id";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getHeader() { return header; }
        public void setHeader(String v) { this.header = v; }
    }

    public static class Logging {
        private boolean json = true;
        private boolean maskSensitive = true;

        public boolean isJson() { return json; }
        public void setJson(boolean v) { this.json = v; }
        public boolean isMaskSensitive() { return maskSensitive; }
        public void setMaskSensitive(boolean v) { this.maskSensitive = v; }
    }

    public static class Metrics {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
    }

    public static class Tracing {
        private boolean enabled = true;
        private double samplingProbability = 0.1;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public double getSamplingProbability() { return samplingProbability; }
        public void setSamplingProbability(double v) { this.samplingProbability = v; }
    }

    public static class Health {
        private final KafkaHealth kafka = new KafkaHealth();
        private final DatabaseHealth database = new DatabaseHealth();

        public KafkaHealth getKafka() { return kafka; }
        public DatabaseHealth getDatabase() { return database; }

        public static class KafkaHealth {
            private boolean enabled = true;
            private Duration timeout = Duration.ofSeconds(3);

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
            public Duration getTimeout() { return timeout; }
            public void setTimeout(Duration d) { this.timeout = d; }
        }

        public static class DatabaseHealth {
            private boolean enabled = true;
            private String validationQuery = "SELECT 1";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
            public String getValidationQuery() { return validationQuery; }
            public void setValidationQuery(String v) { this.validationQuery = v; }
        }
    }
}
