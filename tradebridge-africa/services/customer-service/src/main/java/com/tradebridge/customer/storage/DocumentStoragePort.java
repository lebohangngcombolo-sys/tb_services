package com.tradebridge.customer.storage;

import java.time.Duration;
import java.util.UUID;

/**
 * Storage abstraction for KYC evidence (spec §6.4 "Store files outside
 * PostgreSQL"). Implementations persist bytes to object storage and
 * return opaque references; the DB stores only metadata.
 *
 * <p>Design intent:
 * <ul>
 *   <li>Providers are pluggable (MinIO for local, S3 for production).</li>
 *   <li>Presigned URLs are the only way clients upload/download.</li>
 *   <li>Server-side encryption and checksums are mandatory.</li>
 * </ul>
 */
public interface DocumentStoragePort {

    /**
     * Generate a new storage key for a document belonging to a case.
     * The key is opaque and stable; the caller persists it as
     * {@code kyc_documents.storage_reference}.
     */
    String newObjectKey(UUID kycCaseId, String requirementCode);

    /**
     * Generate a presigned URL for a client to upload bytes directly.
     * URL validity is bounded by {@code expiry} (typically 15m).
     */
    PresignedUpload generateUploadUrl(String objectKey, String contentType,
                                       long contentLength, Duration expiry);

    /**
     * Generate a presigned URL for download. Access is short-lived.
     */
    String generateDownloadUrl(String objectKey, Duration expiry);

    /**
     * Delete an object (used on document removal / retention expiry).
     */
    void delete(String objectKey);

    /**
     * Verify that the object exists and matches the expected checksum.
     * Called by the async scan / post-upload verification hook.
     */
    boolean verifyIntegrity(String objectKey, String expectedSha256);

    /** Presigned upload response. */
    record PresignedUpload(
            String url,
            String objectKey,
            java.util.Map<String, String> requiredHeaders,
            java.time.Instant expiresAt
    ) { }
}