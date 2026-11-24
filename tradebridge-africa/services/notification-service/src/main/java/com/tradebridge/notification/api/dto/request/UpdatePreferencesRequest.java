package com.tradebridge.notification.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdatePreferencesRequest(
        @NotEmpty List<@Valid ChannelPreferenceRequest> preferences
) { }