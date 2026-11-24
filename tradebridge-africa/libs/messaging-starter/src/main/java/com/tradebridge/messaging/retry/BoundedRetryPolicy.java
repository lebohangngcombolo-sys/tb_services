package com.tradebridge.messaging.retry;

import com.tradebridge.messaging.config.MessagingProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * Exponential backoff with a hard attempt cap. Attempt numbers are 1-based.
 *
 * <p>Delay = min(initial * multiplier^(attempt-1), maxBackoff).
 *
 * <p>Business-error outcomes should NOT be retried (spec §5.4: "bounded
 * retries"); the consumer layer should classify failures as
 * {@code Retryable} vs {@code Terminal} before calling this policy.
 */
public final class BoundedRetryPolicy {

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final double multiplier;

    public BoundedRetryPolicy(int maxAttempts, Duration initialBackoff,
                              Duration maxBackoff, double multiplier) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (multiplier < 1.0) throw new IllegalArgumentException("multiplier must be >= 1.0");
        this.maxAttempts = maxAttempts;
        this.initialBackoff = Objects.requireNonNull(initialBackoff);
        this.maxBackoff = Objects.requireNonNull(maxBackoff);
        this.multiplier = multiplier;
    }

    public static BoundedRetryPolicy from(MessagingProperties.Retry p) {
        return new BoundedRetryPolicy(p.getMaxAttempts(), p.getInitialBackoff(),
                p.getMaxBackoff(), p.getMultiplier());
    }

    public int maxAttempts() { return maxAttempts; }

    public boolean canRetry(int attemptsSoFar) {
        return attemptsSoFar < maxAttempts;
    }

    public Duration delayFor(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
        long baseMs = initialBackoff.toMillis();
        double scaled = baseMs * Math.pow(multiplier, attempt - 1);
        long capped = Math.min((long) scaled, maxBackoff.toMillis());
        return Duration.ofMillis(capped);
    }
}
