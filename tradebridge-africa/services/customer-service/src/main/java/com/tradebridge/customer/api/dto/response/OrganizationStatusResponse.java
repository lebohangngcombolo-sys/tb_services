package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrganizationStatusResponse(
        UUID organizationId,
        String status,
        UUID activeKycCaseId,
        String activeKycCaseStatus,
        Instant lastStatusChangeAt
) { }