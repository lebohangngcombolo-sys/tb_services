package com.tradebridge.observability.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Health indicator that pings Kafka by describing the cluster.
 *
 * <p>Registered only when {@code spring.kafka.bootstrap-servers} is
 * configured and {@code kafka-clients} is on the classpath.
 *
 * <p>Timeout is bounded so that a slow broker does not stall the
 * actuator endpoint. Detailed error text is masked — do not leak
 * credentials in broker hostnames.
 */
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(3);

    private final AdminClient adminClient;

    public KafkaHealthIndicator(String bootstrapServers) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) ADMIN_TIMEOUT.toMillis());
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) ADMIN_TIMEOUT.toMillis());
        this.adminClient = AdminClient.create(props);
    }

    @Override
    public Health health() {
        try {
            DescribeClusterResult result = adminClient.describeCluster(
                    new DescribeClusterOptions().timeoutMs((int) ADMIN_TIMEOUT.toMillis()));
            String clusterId = result.clusterId().get(ADMIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            int nodeCount = result.nodes().get(ADMIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).size();
            return Health.up()
                    .withDetail("clusterId", clusterId == null ? "unknown" : clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", safeMessage(e))
                    .build();
        }
    }

    private static String safeMessage(Exception e) {
        String message = e.getClass().getSimpleName();
        if (e.getMessage() != null && !e.getMessage().contains("password")) {
            message += ": " + e.getMessage();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
