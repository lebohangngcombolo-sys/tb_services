package com.tradebridge.country.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request to publish a new configuration version. If the country has a
 * DRAFT configuration, the request may reference it by {@code draftVersion}
 * or provide an inline change summary. The exact rules are handled by
 * {@code ConfigurationService#publish}.
 */
public record PublishConfigurationRequest(
        @NotBlank @Size(max = 500) String changeSummary,
        Map<String, Object> configuration
) { }