package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.ownership.declaration.submitted.v1} payload (spec §6.3).
 */
public record OwnershipDeclarationSubmittedPayload(
        UUID organizationId
) { }