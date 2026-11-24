package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnershipDeclarationRequest(
        @NotBlank @Size(max = 500) String declaration,
        boolean noBeneficialOwnersAboveThreshold
) { }