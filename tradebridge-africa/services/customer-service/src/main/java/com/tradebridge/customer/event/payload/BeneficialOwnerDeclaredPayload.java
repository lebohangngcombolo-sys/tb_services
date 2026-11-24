package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.beneficial.owner.declared.v1} payload (spec §6.3).
 */
public record BeneficialOwnerDeclaredPayload(
        UUID organizationId,
        UUID beneficialOwnerId
) { }