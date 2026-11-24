package com.tradebridge.notification.api.dto.request;

import com.tradebridge.common.enums.Channel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChannelPreferenceRequest(
        @NotNull Channel channel,
        @NotNull Boolean enabled,
        @Size(max = 5) String languageCode
) { }