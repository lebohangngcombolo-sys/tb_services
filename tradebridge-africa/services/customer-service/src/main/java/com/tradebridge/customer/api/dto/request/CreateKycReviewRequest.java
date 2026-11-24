package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateKycReviewRequest(
        @NotBlank @Pattern(regexp = "^(INITIAL|SECONDARY|ENHANCED)$") String reviewStage,
        @Size(max = 2000) String notes
) { }