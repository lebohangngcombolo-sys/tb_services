package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.kyc.additional.information.requested.v1} payload (spec §6.3).
 */
public record KycAdditionalInfoRequestedPayload(
        UUID kycCaseId,
        String reasonCode
) { }