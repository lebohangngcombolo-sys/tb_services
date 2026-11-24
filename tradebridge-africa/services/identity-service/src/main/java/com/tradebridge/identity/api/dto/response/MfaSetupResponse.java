package com.tradebridge.identity.api.dto.response;

/**
 * Response for beginning TOTP setup.
 *
 * <p>Returns the provisioning URI (for QR display) and a masked secret.
 * The secret itself is only shown once; subsequent views must not
 * reveal it. Do not log this payload.
 */
public record MfaSetupResponse(
        String provisioningUri,
        String secretMasked,
        String issuer,
        String accountName
) { }