package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.mfa.otp.requested.v1} payload.
 *
 * <p>Contains only the challenge ID and masked destination — the OTP code
 * itself is delivered through the protected handoff mechanism
 * (spec §3.3).
 */
public record MfaOtpRequestedPayload(
        String userId,
        String challengeId,
        String destinationMasked
) { }