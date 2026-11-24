package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawOrganizationRequest(
        @NotBlank @Size(max = 500) String reason
) { }