package com.tradebridge.notification.provider;

import com.tradebridge.notification.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of a single provider send attempt.
 *
 * <p>{@code ACCEPTED} means the provider has taken responsibility; whether
 * the message reaches the recipient may be confirmed by a callback
 * (spec §5.3: "A provider accepting a message is not always proof that the
 * recipient received it").
 */
public record ProviderResult(
        boolean success,
        DeliveryStatus status,
        String providerName,
        String providerMessageId,
        String failureCode,
        String failureReason,
        Instant submittedAt
) {
    public ProviderResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(providerName, "providerName");
    }

    public static ProviderResult accepted(String providerName, String providerMessageId) {
        return new ProviderResult(true, DeliveryStatus.ACCEPTED,
                providerName, providerMessageId, null, null, Instant.now());
    }

    public static ProviderResult failed(String providerName, String code, String reason) {
        return new ProviderResult(false, DeliveryStatus.FAILED,
                providerName, null, code, reason, Instant.now());
    }
}