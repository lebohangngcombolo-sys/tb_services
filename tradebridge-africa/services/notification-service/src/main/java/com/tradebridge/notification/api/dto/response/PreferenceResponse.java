package com.tradebridge.notification.api.dto.response;

public record PreferenceResponse(
        String channel,
        boolean enabled,
        String languageCode,
        boolean mandatory
) { }