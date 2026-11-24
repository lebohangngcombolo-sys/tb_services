package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        UUID identityUserId,
        String customerNumber,
        String customerType,
        String countryCode,
        String preferredLanguage,
        String status,
        Instant createdAt,
        Instant updatedAt
) { }