package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.organization.updated.v1} payload (spec §6.3).
 * {@code changedFields} is a comma-separated list.
 */
public record OrganizationUpdatedPayload(
        UUID organizationId,
        String changedFields
) { }