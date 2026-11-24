package com.tradebridge.notification.mapper;

import com.tradebridge.notification.api.dto.response.PreviewResponse;
import com.tradebridge.notification.api.dto.response.TemplateResponse;
import com.tradebridge.notification.domain.NotificationTemplate;

public final class TemplateMapper {

    public TemplateResponse toResponse(NotificationTemplate t) {
        if (t == null) return null;
        return new TemplateResponse(
                t.getId(),
                t.getTemplateCode(),
                t.getChannel().name(),
                t.getLanguageCode(),
                t.getSubjectTemplate(),
                t.getBodyTemplate(),
                t.getVariablesSchema(),
                t.getVersion(),
                t.isEnabled(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    public PreviewResponse toPreview(String subject, String body) {
        return new PreviewResponse(subject, body);
    }
}