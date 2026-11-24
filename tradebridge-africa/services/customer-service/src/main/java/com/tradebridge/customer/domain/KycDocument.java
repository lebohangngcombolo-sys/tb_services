package com.tradebridge.customer.domain;

import com.tradebridge.customer.domain.enums.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * KYC document metadata (spec §6.2 {@code kyc_documents}).
 *
 * <p>Stores only metadata + storage reference. Actual bytes live in MinIO
 * (spec §6.4 "Store files outside PostgreSQL").
 */
@Entity
@Table(name = "kyc_documents")
public class KycDocument {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "kyc_case_id", nullable = false, updatable = false)
    private UUID kycCaseId;

    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(name = "requirement_code", nullable = false, length = 100)
    private String requirementCode;

    @Column(name = "storage_reference", nullable = false, length = 500)
    private String storageReference;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 30)
    private DocumentStatus documentStatus;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected KycDocument() { }

    public KycDocument(UUID id, UUID kycCaseId, String documentType, String requirementCode,
                       String storageReference, String originalFilename, String mimeType,
                       long fileSizeBytes, String checksum, UUID uploadedByUserId,
                       Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.kycCaseId = Objects.requireNonNull(kycCaseId, "kycCaseId");
        this.documentType = Objects.requireNonNull(documentType, "documentType");
        this.requirementCode = Objects.requireNonNull(requirementCode, "requirementCode");
        this.storageReference = Objects.requireNonNull(storageReference, "storageReference");
        this.originalFilename = originalFilename;
        this.mimeType = Objects.requireNonNull(mimeType, "mimeType");
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
        this.documentStatus = DocumentStatus.PENDING;
        this.uploadedByUserId = Objects.requireNonNull(uploadedByUserId, "uploadedByUserId");
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    // ------------------------------------------------------------------ behavior

    public void markScanning() { this.documentStatus = DocumentStatus.SCANNING; }
    public void accept() { this.documentStatus = DocumentStatus.ACCEPTED; }
    public void reject() { this.documentStatus = DocumentStatus.REJECTED; }
    public void expire() { this.documentStatus = DocumentStatus.EXPIRED; }

    public boolean isExpiredAt(Instant when) {
        return expiresAt != null && when.isAfter(expiresAt);
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public UUID getKycCaseId() { return kycCaseId; }
    public String getDocumentType() { return documentType; }
    public String getRequirementCode() { return requirementCode; }
    public String getStorageReference() { return storageReference; }
    public String getOriginalFilename() { return originalFilename; }
    public String getMimeType() { return mimeType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getChecksum() { return checksum; }
    public DocumentStatus getDocumentStatus() { return documentStatus; }
    public UUID getUploadedByUserId() { return uploadedByUserId; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}