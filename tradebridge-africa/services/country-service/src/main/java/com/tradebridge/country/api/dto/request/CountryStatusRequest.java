package com.tradebridge.country.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record CountryStatusRequest(@NotNull Boolean enabled) { }