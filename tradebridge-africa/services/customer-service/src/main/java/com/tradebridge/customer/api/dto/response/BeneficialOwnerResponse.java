package com.tradebridge.customer.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BeneficialOwnerResponse(
        UUID id,
        UUID organizationId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String nationalityCountryCode,
        String residenceCountryCode,
        BigDecimal ownershipPercentage,
        String controlType,
        String verificationStatus,
        Instant createdAt,
        Instant updatedAt
) { }