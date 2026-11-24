package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.organization.submitted.v1} payload (spec §6.3).
 * Includes {@code configurationVersion} so consumers know which country
 * rules applied at submission (spec §11 decision 3).
 */
public record OrganizationSubmittedPayload(
        UUID organizationId,
        UUID kycCaseId,
        int configurationVersion
) { }