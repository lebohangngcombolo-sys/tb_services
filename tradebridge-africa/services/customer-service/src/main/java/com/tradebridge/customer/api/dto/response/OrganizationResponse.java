package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String organizationNumber,
        String legalName,
        String tradingName,
        String organizationType,
        String registrationNumber,
        String taxNumber,
        String countryCode,
        LocalDate incorporationDate,
        String registrationAuthority,
        String businessDescription,
        String website,
        String status,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt
) { }