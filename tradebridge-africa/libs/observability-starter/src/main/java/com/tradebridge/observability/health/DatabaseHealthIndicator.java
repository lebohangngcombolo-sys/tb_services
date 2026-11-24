package com.tradebridge.observability.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;

/**
 * Health indicator that executes a lightweight query against the
 * configured {@link DataSource}.
 *
 * <p>Registered only when a {@code DataSource} bean exists and
 * {@code spring-jdbc} is on the classpath. Uses {@code SELECT 1} by
 * default. Never logs the datasource URL — it may include credentials.
 */
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(2);

    private final DataSource dataSource;
    private final String validationQuery;

    public DatabaseHealthIndicator(DataSource dataSource, String validationQuery) {
        this.dataSource = dataSource;
        this.validationQuery = validationQuery == null ? "SELECT 1" : validationQuery;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout((int) QUERY_TIMEOUT.toSeconds());
            stmt.execute(validationQuery);
            return Health.up()
                    .withDetail("database", conn.getCatalog() == null ? "default" : conn.getCatalog())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", truncate(e.getMessage()))
                    .build();
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
