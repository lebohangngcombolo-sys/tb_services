package com.tradebridge.customer.api.dto.request;

import com.tradebridge.validation.annotation.ValidE164;
import com.tradebridge.validation.annotation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddRepresentativeRequest(
        UUID identityUserId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @ValidEmail String email,
        @ValidE164 String phoneNumber,
        @NotBlank @Pattern(regexp = "^(DIRECTOR|OFFICER|SHAREHOLDER|SIGNATORY|OTHER)$")
        String representativeType,
        boolean authorizedSignatory
) { }