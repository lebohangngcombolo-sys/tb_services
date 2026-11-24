package com.tradebridge.notification.api.dto.response;

public record ProviderStatusResponse(
        String name,
        String channel,
        boolean enabled,
        boolean healthy,
        String detail
) { }