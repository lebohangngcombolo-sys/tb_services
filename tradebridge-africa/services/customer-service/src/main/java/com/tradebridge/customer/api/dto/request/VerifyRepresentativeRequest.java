package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyRepresentativeRequest(
        @NotBlank @Pattern(regexp = "^(VERIFIED|REJECTED)$") String status,
        @Size(max = 500) String reason
) { }