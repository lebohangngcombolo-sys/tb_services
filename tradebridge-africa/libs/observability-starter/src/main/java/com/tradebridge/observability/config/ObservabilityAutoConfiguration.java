package com.tradebridge.observability.config;

import com.tradebridge.observability.correlation.CorrelationIdFilter;
import com.tradebridge.observability.health.DatabaseHealthIndicator;
import com.tradebridge.observability.health.KafkaHealthIndicator;
import com.tradebridge.observability.metrics.CommonTagsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaAdmin;

import javax.sql.DataSource;

/**
 * Root auto-configuration for the observability starter.
 *
 * <p>Wires:
 * <ul>
 *   <li>{@link CorrelationIdFilter} (servlet only)</li>
 *   <li>{@link CommonTagsProvider} + Micrometer customizer</li>
 *   <li>{@link KafkaHealthIndicator} (when Kafka is on the classpath + bootstrap servers set)</li>
 *   <li>{@link DatabaseHealthIndicator} (when a {@link DataSource} bean exists)</li>
 * </ul>
 *
 * <p>Tracing infrastructure is provided by Spring Boot's own auto-config
 * (micrometer-tracing-bridge-otel), which activates automatically when the
 * tracer is on the classpath. {@link com.tradebridge.observability.tracing.OpenTelemetryConfig}
 * is imported explicitly to attach the observation handlers.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    // ------------------------------------------------------------- correlation

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "tradebridge.observability.correlation", name = "enabled",
            matchIfMissing = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    // ------------------------------------------------------------- metrics

    @Bean
    @ConditionalOnMissingBean
    public CommonTagsProvider commonTagsProvider(ObservabilityProperties properties) {
        return new CommonTagsProvider(
                properties.getServiceName(),
                properties.getEnvironment(),
                properties.getRegion());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    public MeterRegistryCustomizer<MeterRegistry> commonTagsCustomizer(CommonTagsProvider provider) {
        return registry -> registry.config().commonTags(provider.commonTags());
    }

    // ------------------------------------------------------------- health

    @Bean
    @ConditionalOnMissingBean(name = "kafkaHealthIndicator")
    @ConditionalOnClass(name = "org.apache.kafka.clients.admin.AdminClient")
    @ConditionalOnProperty(prefix = "tradebridge.observability.health.kafka", name = "enabled",
            matchIfMissing = true)
    public HealthIndicator kafkaHealthIndicator(
            ObservabilityProperties properties,
            @Value("${spring.kafka.bootstrap-servers:}") String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            // No Kafka configured — expose "up" but with a note
            return () -> org.springframework.boot.actuate.health.Health.up()
                    .withDetail("kafka", "not configured")
                    .build();
        }
        return new KafkaHealthIndicator(bootstrapServers);
    }

    @Bean
    @ConditionalOnMissingBean(name = "databaseHealthIndicator")
    @ConditionalOnClass(name = "javax.sql.DataSource")
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(prefix = "tradebridge.observability.health.database", name = "enabled",
            matchIfMissing = true)
    public HealthIndicator databaseHealthIndicator(DataSource dataSource,
                                                   ObservabilityProperties properties) {
        return new DatabaseHealthIndicator(dataSource,
                properties.getHealth().getDatabase().getValidationQuery());
    }
}
