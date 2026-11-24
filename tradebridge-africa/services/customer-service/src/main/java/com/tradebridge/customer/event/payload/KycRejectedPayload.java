package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.kyc.rejected.v1} payload (spec §6.3).
 */
public record KycRejectedPayload(
        UUID organizationId,
        UUID kycCaseId,
        UUID decisionId
) { }