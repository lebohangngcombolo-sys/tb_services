package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record KycDocumentResponse(
        UUID id,
        UUID kycCaseId,
        String documentType,
        String requirementCode,
        String storageReference,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        String checksum,
        String documentStatus,
        UUID uploadedByUserId,
        Instant uploadedAt,
        Instant expiresAt
) { }