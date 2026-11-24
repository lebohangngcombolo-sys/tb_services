package com.tradebridge.customer.api.dto.request;

import com.tradebridge.validation.annotation.ValidCountryCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 255) String legalName,
        @Size(max = 255) String tradingName,
        @NotNull @Size(max = 50) String organizationType,
        @NotBlank @Size(max = 100) String registrationNumber,
        @Size(max = 100) String taxNumber,
        @NotNull @ValidCountryCode(allowAlpha3 = false) String countryCode,
        @PastOrPresent LocalDate incorporationDate,
        @Size(max = 150) String registrationAuthority,
        String businessDescription,
        @Pattern(regexp = "^https?://.*$", message = "Must be a valid URL")
        @Size(max = 255) String website
) { }