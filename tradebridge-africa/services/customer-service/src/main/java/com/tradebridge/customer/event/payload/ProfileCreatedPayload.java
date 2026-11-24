package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.profile.created.v1} payload (spec §6.3).
 */
public record ProfileCreatedPayload(
        UUID customerId,
        UUID userId,
        String countryCode
) { }