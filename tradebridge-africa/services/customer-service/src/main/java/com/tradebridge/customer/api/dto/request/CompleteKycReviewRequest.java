package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteKycReviewRequest(
        @NotBlank @Pattern(regexp = "^(APPROVED|REJECTED|NEEDS_INFO)$") String outcome,
        @Size(max = 2000) String notes
) { }