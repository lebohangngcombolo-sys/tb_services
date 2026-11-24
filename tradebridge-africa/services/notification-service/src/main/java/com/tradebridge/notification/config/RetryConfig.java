package com.tradebridge.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Bounded retry policy for notification delivery (spec §5.4 "Delivery
 * retries must use bounded exponential backoff").
 *
 * <p>The retry policy governs *in-call* retries against a single provider
 * endpoint (e.g. transient network errors during a send). Cross-attempt
 * retries that create new delivery records are handled by
 * {@code RetryService} using the same exponential parameters.
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate notificationRetryTemplate(NotificationProperties properties) {
        RetryTemplate template = new RetryTemplate();

        SimpleRetryPolicy policy = new SimpleRetryPolicy(
                properties.getRetry().getMaxAttempts());

        ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(properties.getRetry().getInitialBackoff().toMillis());
        backoff.setMaxInterval(properties.getRetry().getMaxBackoff().toMillis());
        backoff.setMultiplier(properties.getRetry().getMultiplier());

        template.setRetryPolicy(policy);
        template.setBackOffPolicy(backoff);
        return template;
    }
}