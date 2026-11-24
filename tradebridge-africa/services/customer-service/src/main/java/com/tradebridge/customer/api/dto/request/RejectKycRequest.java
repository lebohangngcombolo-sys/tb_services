package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectKycRequest(
        @NotBlank @Size(max = 100) String reasonCode,
        @NotBlank @Size(max = 2000) String reason
) { }