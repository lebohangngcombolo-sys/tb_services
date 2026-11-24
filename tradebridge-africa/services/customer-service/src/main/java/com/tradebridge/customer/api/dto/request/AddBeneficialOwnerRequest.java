package com.tradebridge.customer.api.dto.request;

import com.tradebridge.validation.annotation.ValidCountryCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddBeneficialOwnerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Past LocalDate dateOfBirth,
        @ValidCountryCode(allowAlpha3 = false) String nationalityCountryCode,
        @ValidCountryCode(allowAlpha3 = false) String residenceCountryCode,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal ownershipPercentage,
        @NotBlank @Pattern(regexp = "^(DIRECT|INDIRECT|CONTROL|TRUST|NOMINEE)$")
        String controlType,
        String identityReference
) { }