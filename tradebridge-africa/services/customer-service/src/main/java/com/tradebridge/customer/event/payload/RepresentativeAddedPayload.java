package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.representative.added.v1} payload (spec §6.3).
 */
public record RepresentativeAddedPayload(
        UUID organizationId,
        UUID representativeId
) { }