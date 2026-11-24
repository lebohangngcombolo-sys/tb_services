package com.tradebridge.identity.domain.enums;

/**
 * Security-sensitive actions recorded in {@code identity_audit_records}
 * (spec §3.2). A future centralized Audit Service can consume the
 * corresponding events independently.
 *
 * <p>Naming convention: {@code <resource>.<action>} so the audit trail
 * is filterable by prefix.
 */
public enum IdentityAction {
    USER_REGISTERED,
    USER_EMAIL_VERIFIED,
    USER_ACTIVATED,
    USER_LOGIN_SUCCESS,
    USER_LOGIN_FAILURE,
    USER_LOGOUT,
    USER_LOGOUT_ALL,
    USER_STATUS_CHANGED,
    USER_UNLOCKED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    PASSWORD_CHANGED,
    MFA_TOTP_SETUP_STARTED,
    MFA_TOTP_ENABLED,
    MFA_DISABLED,
    MFA_CHALLENGE_ISSUED,
    MFA_CHALLENGE_FAILED,
    MFA_CHALLENGE_PASSED,
    RECOVERY_CODES_REGENERATED,
    ROLE_ASSIGNED,
    ROLE_REMOVED,
    SESSION_REVOKED,
    SESSION_REVOKED_ALL
}