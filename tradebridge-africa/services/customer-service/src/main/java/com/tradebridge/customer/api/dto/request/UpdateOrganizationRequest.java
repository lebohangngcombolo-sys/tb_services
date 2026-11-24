package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateOrganizationRequest(
        @Size(max = 255) String legalName,
        @Size(max = 255) String tradingName,
        @Size(max = 100) String taxNumber,
        @PastOrPresent LocalDate incorporationDate,
        @Size(max = 150) String registrationAuthority,
        String businessDescription,
        @Pattern(regexp = "^https?://.*$", message = "Must be a valid URL")
        @Size(max = 255) String website
) { }