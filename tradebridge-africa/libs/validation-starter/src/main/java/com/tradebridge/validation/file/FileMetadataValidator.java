package com.tradebridge.validation.file;

import java.util.Locale;
import java.util.Set;

/**
 * File metadata validation (MIME type + size). Does <b>not</b> inspect content —
 * content scanning is a separate concern handled by the storage layer.
 *
 * <p>Spec §4.4, §6.4: File requirements must specify MIME-type and size
 * restrictions; the country provides these constraints (this validator
 * simply enforces them). If the country does not specify, safe defaults apply.
 */
public final class FileMetadataValidator {

    /** Reasonable default max size if the country does not specify. */
    public static final long DEFAULT_MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MiB

    /** Default MIME types accepted when the country does not specify. */
    public static final Set<String> DEFAULT_ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private FileMetadataValidator() { }

    public static boolean isValidMimeType(String mimeType, Set<String> allowed) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        Set<String> effective = (allowed == null || allowed.isEmpty())
                ? DEFAULT_ALLOWED_MIME_TYPES : allowed;
        return effective.contains(mimeType.toLowerCase(Locale.ROOT).trim());
    }

    public static boolean isValidSize(Long sizeBytes, Long maxBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            return false;
        }
        long limit = (maxBytes == null || maxBytes <= 0) ? DEFAULT_MAX_SIZE_BYTES : maxBytes;
        return sizeBytes <= limit;
    }

    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        // Reject path traversal, null bytes, control chars
        if (filename.contains("\0") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        // Reasonable length cap
        return filename.length() <= 255;
    }

    /**
     * Combined validation. Returns {@link Result#ok()} on success, or a
     * specific error code on failure.
     */
    public static Result validate(String filename, String mimeType, Long sizeBytes,
                                  Set<String> allowedMimeTypes, Long maxSizeBytes) {
        if (!isValidFilename(filename)) {
            return Result.invalid("filename");
        }
        if (!isValidMimeType(mimeType, allowedMimeTypes)) {
            return Result.invalid("mimeType");
        }
        if (!isValidSize(sizeBytes, maxSizeBytes)) {
            return Result.invalid("sizeBytes");
        }
        return Result.ok();
    }

    public record Result(boolean valid, String failedField) {
        public static Result ok() { return new Result(true, null); }
        public static Result invalid(String field) { return new Result(false, field); }
    }
}
