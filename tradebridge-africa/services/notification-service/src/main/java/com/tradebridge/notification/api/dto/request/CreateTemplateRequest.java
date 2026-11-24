package com.tradebridge.notification.api.dto.request;

import com.tradebridge.common.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateTemplateRequest(
        @NotBlank @Size(max = 100) String templateCode,
        @NotNull Channel channel,
        @NotBlank @Size(max = 5) String languageCode,
        @Size(max = 500) String subjectTemplate,
        @NotBlank String bodyTemplate,
        Map<String, Object> variablesSchema,
        @NotNull Boolean enabled
) { }