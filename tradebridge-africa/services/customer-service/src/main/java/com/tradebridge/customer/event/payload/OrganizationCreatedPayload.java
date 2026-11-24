package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.organization.created.v1} payload (spec §6.3).
 */
public record OrganizationCreatedPayload(
        UUID organizationId,
        String countryCode
) { }