package com.tradebridge.customer.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.customer.api.dto.request.RegisterKycDocumentRequest;
import com.tradebridge.customer.api.dto.response.KycDocumentResponse;
import com.tradebridge.customer.client.CountryServiceClient;
import com.tradebridge.customer.client.dto.KycRequirementsResponse;
import com.tradebridge.customer.config.CustomerProperties;
import com.tradebridge.customer.domain.KycCase;
import com.tradebridge.customer.domain.KycDocument;
import com.tradebridge.customer.domain.Organization;
import com.tradebridge.customer.domain.enums.DocumentStatus;
import com.tradebridge.customer.mapper.KycCaseMapper;
import com.tradebridge.customer.repository.KycCaseRepository;
import com.tradebridge.customer.repository.KycDocumentRepository;
import com.tradebridge.customer.repository.OrganizationRepository;
import com.tradebridge.customer.repository.OrganizationRepresentativeRepository;
import com.tradebridge.customer.storage.DocumentStoragePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * KYC document metadata (spec §6.1E). Bytes live in MinIO — this class
 * only manages metadata, validates against the country's requirements,
 * and hands off to {@link DocumentStoragePort} for signed-URL issuance.
 */
@Service
public class KycDocumentService {

    private final KycDocumentRepository documentRepository;
    private final KycCaseRepository kycCaseRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationRepresentativeRepository representativeRepository;
    private final CountryServiceClient countryServiceClient;
    private final DocumentStoragePort storage;
    private final CustomerProperties properties;
    private final KycCaseMapper mapper;

    public KycDocumentService(KycDocumentRepository documentRepository,
                                KycCaseRepository kycCaseRepository,
                                OrganizationRepository organizationRepository,
                                OrganizationRepresentativeRepository representativeRepository,
                                CountryServiceClient countryServiceClient,
                                DocumentStoragePort storage,
                                CustomerProperties properties,
                                KycCaseMapper mapper) {
        this.documentRepository = documentRepository;
        this.kycCaseRepository = kycCaseRepository;
        this.organizationRepository = organizationRepository;
        this.representativeRepository = representativeRepository;
        this.countryServiceClient = countryServiceClient;
        this.storage = storage;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Transactional
    public KycDocumentResponse register(UUID caseId, UUID userId,
                                          RegisterKycDocumentRequest request) {
        KycCase kycCase = kycCaseRepository.findById(caseId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycCase", caseId));
        Organization org = organizationRepository.findById(kycCase.getOrganizationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Organization",
                        kycCase.getOrganizationId()));
        requireAuthorizedRepresentative(org, userId);

        if (kycCase.getStatus().isTerminal()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Case is already " + kycCase.getStatus());
        }

        // Look up country requirement and validate
        KycRequirementsResponse requirements = countryServiceClient
                .getKycRequirements(org.getCountryCode(), "COMPANY");
        KycRequirementsResponse.Item requirement = requirements.requirements().stream()
                .filter(r -> r.requirementCode().equals(request.requirementCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Requirement code not configured for this country: "
                                + request.requirementCode()));

        if (requirement.allowedMimeTypes() != null
                && !requirement.allowedMimeTypes().isEmpty()
                && !requirement.allowedMimeTypes().contains(request.mimeType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "MIME type " + request.mimeType() + " is not allowed for this requirement");
        }
        Long maxSize = requirement.maxFileSizeBytes() != null
                ? requirement.maxFileSizeBytes()
                : properties.getDocuments().getMaxFileSizeBytes();
        if (request.fileSizeBytes() > maxSize) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "File exceeds the maximum size of " + maxSize + " bytes");
        }

        // Generate storage reference. The actual bytes are uploaded via the
        // signed URL issued by the storage adapter (see below).
        String storageReference = storage.newObjectKey(caseId, request.requirementCode());
        Instant expiresAt = requirement.validityDays() != null
                ? Instant.now().plusSeconds(requirement.validityDays() * 86400L)
                : null;

        KycDocument doc = new KycDocument(
                UuidV7Generator.next(),
                caseId,
                request.documentType(),
                request.requirementCode(),
                storageReference,
                request.originalFilename(),
                request.mimeType(),
                request.fileSizeBytes(),
                request.checksum(),
                userId,
                expiresAt);
        // Optimistically mark as ACCEPTED — real scan integration would keep
        // it SCANNING then transition to ACCEPTED via a callback.
        doc.accept();
        documentRepository.save(doc);

        return mapper.toDocumentResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> list(UUID caseId, UUID userId) {
        KycCase kycCase = kycCaseRepository.findById(caseId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycCase", caseId));
        Organization org = organizationRepository.findById(kycCase.getOrganizationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Organization",
                        kycCase.getOrganizationId()));
        requireMemberAccess(org, userId);
        return documentRepository.findByKycCaseIdOrderByUploadedAtDesc(caseId).stream()
                .map(mapper::toDocumentResponse).toList();
    }

    @Transactional
    public void remove(UUID caseId, UUID documentId, UUID userId) {
        KycCase kycCase = kycCaseRepository.findById(caseId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycCase", caseId));
        Organization org = organizationRepository.findById(kycCase.getOrganizationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Organization",
                        kycCase.getOrganizationId()));
        requireAuthorizedRepresentative(org, userId);

        KycDocument doc = documentRepository.findByIdAndKycCaseId(documentId, caseId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycDocument", documentId));

        if (doc.getDocumentStatus() == DocumentStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Accepted documents cannot be removed — contact support");
        }
        documentRepository.delete(doc);
    }

    // ------------------------------------------------------------------ helpers

    private void requireAuthorizedRepresentative(Organization org, UUID userId) {
        if (org.getCreatedByUserId().equals(userId)) return;
        boolean authorized = representativeRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(r -> userId.equals(r.getIdentityUserId()) && r.isAuthorizedSignatory());
        if (!authorized) {
            throw new ForbiddenException(ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                    "User is not an authorized representative");
        }
    }

    private void requireMemberAccess(Organization org, UUID userId) {
        if (org.getCreatedByUserId().equals(userId)) return;
        boolean isMember = representativeRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(r -> userId.equals(r.getIdentityUserId()));
        if (!isMember) {
            throw new ForbiddenException(ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                    "User is not a member of this organization");
        }
    }
}