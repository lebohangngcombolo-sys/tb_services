package com.tradebridge.identity.api.dto.response;

/**
 * Response for login/refresh. Access token is a signed JWT; refresh token
 * is an opaque value stored (hashed) in {@code refresh_tokens}.
 *
 * <p>Never include OTPs, secrets, or password material in this payload.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds,
        boolean mfaRequired,
        String mfaChallengeId,
        UserSummary user
) {
    public record UserSummary(
            String id,
            String email,
            String firstName,
            String lastName,
            java.util.List<String> roles
    ) { }
}