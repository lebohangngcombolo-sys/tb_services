package com.tradebridge.identity.domain.enums;

/**
 * MFA factor type (spec §3.2 "Factor types").
 *
 * <p>TOTP is the preferred authenticator method; EMAIL_OTP is secondary
 * (spec §11 decision 2).
 */
public enum MfaFactorType {
    TOTP,
    EMAIL_OTP
}