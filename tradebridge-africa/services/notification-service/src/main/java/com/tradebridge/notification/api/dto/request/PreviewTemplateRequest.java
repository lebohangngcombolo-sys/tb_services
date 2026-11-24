package com.tradebridge.notification.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PreviewTemplateRequest(
        @NotNull Map<String, Object> variables,
        @Size(max = 5) String languageCode
) { }