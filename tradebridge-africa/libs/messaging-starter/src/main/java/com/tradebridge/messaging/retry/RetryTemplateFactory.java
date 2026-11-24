package com.tradebridge.messaging.retry;

import com.tradebridge.messaging.config.MessagingProperties;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Builds a {@link RetryTemplate} honoring the bounded retry policy
 * from {@link MessagingProperties.Retry}.
 */
public final class RetryTemplateFactory {

    private RetryTemplateFactory() { }

    public static RetryTemplate create(MessagingProperties.Retry props) {
        SimpleRetryPolicy policy = new SimpleRetryPolicy(props.getMaxAttempts());

        ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(props.getInitialBackoff().toMillis());
        backoff.setMaxInterval(props.getMaxBackoff().toMillis());
        backoff.setMultiplier(props.getMultiplier());

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(policy);
        template.setBackOffPolicy(backoff);
        return template;
    }
}
