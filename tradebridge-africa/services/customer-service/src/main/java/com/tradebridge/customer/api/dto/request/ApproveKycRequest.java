package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.Size;

public record ApproveKycRequest(
        @Size(max = 2000) String note
) { }