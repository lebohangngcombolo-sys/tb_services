package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateKycCaseRequest(
        @NotBlank @Pattern(regexp = "^(ONBOARDING|PERIODIC_REVIEW|ENHANCED_DUE_DILIGENCE)$")
        String caseType,
        @Size(max = 100) String reason
) { }