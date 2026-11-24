package com.tradebridge.customer.storage;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 checksum helper (spec §6.2 {@code kyc_documents.checksum}).
 *
 * <p>Used by the client upload path and by post-upload integrity checks.
 * Deterministic, encoding-agnostic for byte streams.
 */
@Component
public class ChecksumCalculator {

    public String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String sha256Hex(String value) {
        return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Stream variant for large uploads — computes the checksum without
     * buffering the entire payload in memory.
     */
    public String sha256Hex(InputStream in) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to read stream for checksum", e);
        }
    }
}