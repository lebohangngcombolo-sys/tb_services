package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.organization.withdrawn.v1} payload (spec §6.3).
 */
public record OrganizationWithdrawnPayload(
        UUID organizationId,
        String reason
) { }