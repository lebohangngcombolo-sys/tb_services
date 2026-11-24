package com.tradebridge.identity.domain.enums;

/**
 * Type of short-lived verification challenge (spec §3.2 {@code verification_challenges}).
 *
 * <p>Each type maps to a scenario the challenge serves. The {@code destination_masked}
 * column carries an already-masked destination (email/phone), never plaintext.
 */
public enum ChallengeType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    MFA_EMAIL_OTP,
    MFA_DISABLE,
    RECOVERY_CODE_REGENERATE,
    RECENT_AUTHENTICATION
}