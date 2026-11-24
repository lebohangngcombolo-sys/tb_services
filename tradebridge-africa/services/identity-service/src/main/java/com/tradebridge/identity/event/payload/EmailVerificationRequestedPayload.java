package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.email.verification.requested.v1} payload.
 *
 * <p>Contains the challenge ID and masked destination only. The raw OTP
 * is delivered through the protected handoff mechanism, never in the
 * Kafka payload (spec §3.3).
 */
public record EmailVerificationRequestedPayload(
        String userId,
        String challengeId,
        String destinationMasked
) { }