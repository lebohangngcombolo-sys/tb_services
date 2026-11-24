package com.tradebridge.identity.api.dto.request;

import com.tradebridge.validation.annotation.ValidCountryCode;
import com.tradebridge.validation.annotation.ValidE164;
import com.tradebridge.validation.annotation.ValidEmail;
import com.tradebridge.validation.annotation.ValidLanguageCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/v1/identity/register (spec §3.1A).
 */
public record RegisterRequest(
        @NotBlank @Size(min = 1, max = 100) String firstName,
        @NotBlank @Size(min = 1, max = 100) String lastName,
        @ValidEmail String email,
        @ValidE164 String phoneNumber,
        @NotBlank @Size(min = 12, max = 128) String password,
        @ValidCountryCode(allowAlpha3 = false) String countryCode,
        @ValidLanguageCode(allowBcp47 = false) String preferredLanguage,
        @Pattern(regexp = "^true$", message = "Terms must be accepted") String termsAccepted
) { }