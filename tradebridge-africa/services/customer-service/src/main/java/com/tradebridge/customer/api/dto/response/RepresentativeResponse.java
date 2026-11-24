package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record RepresentativeResponse(
        UUID id,
        UUID organizationId,
        UUID identityUserId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String representativeType,
        boolean authorizedSignatory,
        String verificationStatus,
        Instant createdAt
) { }