package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterKycDocumentRequest(
        @NotBlank @Size(max = 60) String documentType,
        @NotBlank @Size(max = 100) String requirementCode,
        @NotBlank @Size(max = 255) String originalFilename,
        @NotBlank @Size(max = 100) String mimeType,
        @NotNull @Positive Long fileSizeBytes,
        @NotBlank @Size(max = 128) String checksum
) { }