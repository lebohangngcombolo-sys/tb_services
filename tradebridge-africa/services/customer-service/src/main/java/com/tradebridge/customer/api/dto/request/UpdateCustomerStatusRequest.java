package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCustomerStatusRequest(
        @NotBlank @Pattern(regexp = "^(ACTIVE|SUSPENDED|CLOSED)$") String status,
        String reason
) { }