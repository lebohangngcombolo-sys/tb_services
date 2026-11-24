package com.tradebridge.notification.api.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String templateCode,
        String channel,
        String languageCode,
        String subjectTemplate,
        String bodyTemplate,
        Map<String, Object> variablesSchema,
        int version,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) { }