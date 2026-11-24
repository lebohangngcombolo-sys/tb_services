package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.kyc.resubmitted.v1} payload (spec §6.3).
 */
public record KycResubmittedPayload(
        UUID kycCaseId
) { }