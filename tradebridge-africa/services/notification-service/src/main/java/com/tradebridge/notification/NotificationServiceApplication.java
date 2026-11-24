package com.tradebridge.notification;

import com.tradebridge.notification.config.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service — email, SMS and in-app notifications (spec §5).
 *
 * <p>Port 8083. Database {@code tb_notification_db}.
 * Consumes {@code identity.events}, {@code customer.events}, {@code country.events}.
 * Publishes {@code notification.events}.
 *
 * <p>{@code @EnableScheduling} is required by:
 * <ul>
 *   <li>the outbox relay</li>
 *   <li>the delivery retry scheduler</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {"com.tradebridge.notification", "com.tradebridge"})
@EnableConfigurationProperties(NotificationProperties.class)
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}