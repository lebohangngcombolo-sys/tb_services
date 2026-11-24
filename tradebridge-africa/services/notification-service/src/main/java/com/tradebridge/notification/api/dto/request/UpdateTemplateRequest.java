package com.tradebridge.notification.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateTemplateRequest(
        @Size(max = 500) String subjectTemplate,
        @NotBlank String bodyTemplate,
        Map<String, Object> variablesSchema
) { }