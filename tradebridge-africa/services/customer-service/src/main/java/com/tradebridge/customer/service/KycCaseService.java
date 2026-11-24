package com.tradebridge.customer.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.common.pagination.PageMetadata;
import com.tradebridge.common.pagination.PageRequest;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.customer.api.dto.request.ApproveKycRequest;
import com.tradebridge.customer.api.dto.request.AssignReviewerRequest;
import com.tradebridge.customer.api.dto.request.CreateKycCaseRequest;
import com.tradebridge.customer.api.dto.request.RejectKycRequest;
import com.tradebridge.customer.api.dto.request.RequestInformationRequest;
import com.tradebridge.customer.api.dto.response.KycCaseResponse;
import com.tradebridge.customer.api.dto.response.KycRequirementResponse;
import com.tradebridge.customer.client.CountryServiceClient;
import com.tradebridge.customer.client.dto.ActiveConfigurationResponse;
import com.tradebridge.customer.client.dto.KycRequirementsResponse;
import com.tradebridge.customer.domain.KycCase;
import com.tradebridge.customer.domain.Organization;
import com.tradebridge.customer.domain.enums.KycCaseStatus;
import com.tradebridge.customer.domain.enums.KycDecisionType;
import com.tradebridge.customer.event.CustomerEventPublisher;
import com.tradebridge.customer.mapper.KycCaseMapper;
import com.tradebridge.customer.repository.KycCaseRepository;
import com.tradebridge.customer.repository.KycDocumentRepository;
import com.tradebridge.customer.repository.OrganizationRepository;
import com.tradebridge.customer.repository.OrganizationRepresentativeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * KYC case lifecycle (spec §6.1E). Reviews, documents, and decisions are
 * handled by their dedicated services; this class owns state transitions
 * and reviewer assignment.
 */
@Service
public class KycCaseService {

    private final KycCaseRepository kycCaseRepository;
    private final KycDocumentRepository documentRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationRepresentativeRepository representativeRepository;
    private final CountryServiceClient countryServiceClient;
    private final KycDecisionService decisionService;
    private final SeparationOfDutiesEnforcer dutiesEnforcer;
    private final CustomerEventPublisher eventPublisher;
    private final KycCaseMapper mapper;

    public KycCaseService(KycCaseRepository kycCaseRepository,
                           KycDocumentRepository documentRepository,
                           OrganizationRepository organizationRepository,
                           OrganizationRepresentativeRepository representativeRepository,
                           CountryServiceClient countryServiceClient,
                           KycDecisionService decisionService,
                           SeparationOfDutiesEnforcer dutiesEnforcer,
                           CustomerEventPublisher eventPublisher,
                           KycCaseMapper mapper) {
        this.kycCaseRepository = kycCaseRepository;
        this.documentRepository = documentRepository;
        this.organizationRepository = organizationRepository;
        this.representativeRepository = representativeRepository;
        this.countryServiceClient = countryServiceClient;
        this.decisionService = decisionService;
        this.dutiesEnforcer = dutiesEnforcer;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Transactional
    public KycCaseResponse create(UUID organizationId, UUID userId, CreateKycCaseRequest request) {
        Organization org = requireOrg(organizationId);
        requireAuthorizedRepresentative(org, userId);

        // Lock in the active configuration version
        ActiveConfigurationResponse activeConfig =
                countryServiceClient.getActiveConfiguration(org.getCountryCode());

        KycCase kycCase = new KycCase(
                UuidV7Generator.next(),
                organizationId,
                request.caseType(),
                org.getCountryCode(),
                activeConfig.version());
        kycCase.assignCaseNumber(generateCaseNumber());
        kycCaseRepository.save(kycCase);
        return mapper.toResponse(kycCase);
    }

    @Transactional(readOnly = true)
    public List<KycCaseResponse> listForOrganization(UUID organizationId, UUID userId) {
        Organization org = requireOrg(organizationId);
        requireMemberAccess(org, userId);
        return kycCaseRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public KycCaseResponse get(UUID caseId, UUID userId) {
        KycCase c = requireCase(caseId);
        Organization org = requireOrg(c.getOrganizationId());
        requireMemberAccessOrAnalyst(org, userId);
        return mapper.toResponse(c);
    }

    @Transactional(readOnly = true)
    public List<KycRequirementResponse> requirements(UUID caseId) {
        KycCase c = requireCase(caseId);
        Organization org = requireOrg(c.getOrganizationId());
        // Fetch the country's current KYC requirements. In a future
        // iteration, we should fetch the requirements matching the case's
        // country_configuration_version rather than the current active one.
        // For now this is authoritative and conservative (fail-safe).
        KycRequirementsResponse response = countryServiceClient
                .getKycRequirements(org.getCountryCode(), "COMPANY");

        List<KycRequirementResponse> out = new ArrayList<>();
        for (KycRequirementsResponse.Item item : response.requirements()) {
            if (!item.enabled()) continue;
            long accepted = documentRepository.countAcceptedForRequirement(caseId,
                    item.requirementCode());
            out.add(new KycRequirementResponse(
                    item.documentType(),
                    item.requirementCode(),
                    item.description(),
                    item.required(),
                    item.allowedMimeTypes(),
                    item.maxFileSizeBytes(),
                    item.validityDays(),
                    accepted > 0));
        }
        return out;
    }

    @Transactional
    public KycCaseResponse submit(UUID caseId, UUID userId) {
        KycCase c = requireCase(caseId);
        Organization org = requireOrg(c.getOrganizationId());
        requireAuthorizedRepresentative(org, userId);

        if (c.getStatus() != KycCaseStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Only DRAFT cases can be submitted");
        }
        c.submit();
        kycCaseRepository.save(c);
        return mapper.toResponse(c);
    }

    @Transactional
    public KycCaseResponse resubmit(UUID caseId, UUID userId) {
        KycCase c = requireCase(caseId);
        Organization org = requireOrg(c.getOrganizationId());
        requireAuthorizedRepresentative(org, userId);

        if (c.getStatus() != KycCaseStatus.INFORMATION_REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Only INFORMATION_REQUESTED cases can be resubmitted");
        }
        c.resubmit();
        kycCaseRepository.save(c);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycResubmitted(caseId, correlationId);

        return mapper.toResponse(c);
    }

    @Transactional
    public KycCaseResponse requestInformation(UUID caseId, UUID analystId,
                                                RequestInformationRequest request) {
        KycCase c = requireCase(caseId);
        if (c.getStatus() != KycCaseStatus.IN_REVIEW
                && c.getStatus() != KycCaseStatus.RESUBMITTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Can only request information from IN_REVIEW or RESUBMITTED cases");
        }
        c.requestInformation();
        kycCaseRepository.save(c);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycAdditionalInfoRequested(caseId, request.reasonCode(),
                correlationId);

        return mapper.toResponse(c);
    }

    @Transactional
    public KycCaseResponse assign(UUID caseId, UUID supervisorId, AssignReviewerRequest request) {
        KycCase c = requireCase(caseId);
        if (c.getStatus().isTerminal()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Cannot assign a reviewer to a completed case");
        }
        if (c.getStatus() == KycCaseStatus.SUBMITTED
                || c.getStatus() == KycCaseStatus.RESUBMITTED) {
            c.startReview(request.reviewerUserId(), null);
        } else {
            c.assignReviewer(request.reviewerUserId());
        }
        kycCaseRepository.save(c);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycReviewStarted(caseId, request.reviewerUserId(), correlationId);

        return mapper.toResponse(c);
    }

    @Transactional
    public KycCaseResponse approve(UUID caseId, UUID analystId, ApproveKycRequest request) {
        KycCase c = requireCase(caseId);
        Organization org = requireOrg(c.getOrganizationId());

        if (!c.getStatus().canAcceptDecision()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Case cannot be approved in status " + c.getStatus());
        }

        boolean isRep = representativeRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(r -> analystId.equals(r.getIdentityUserId()));
        dutiesEnforcer.requireEligible(analystId, org.getCreatedByUserId(), c, isRep);

        decisionService.recordDecision(caseId, KycDecisionType.APPROVED,
                null, request.note(), analystId);
        c.approve();
        kycCaseRepository.save(c);
        org.markApproved();
        organizationRepository.save(org);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycApproved(org.getId(), caseId,
                decisionService.latestDecisionId(caseId), correlationId);

        return mapper.toResponse(c);
    }

    @Transactional
    public KycCaseResponse reject(UUID caseId, UUID analystId, RejectKycRequest request) {
        KycCase c = requireCase(caseId);
        Organization org = requireOrg(c.getOrganizationId());

        if (!c.getStatus().canAcceptDecision()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Case cannot be rejected in status " + c.getStatus());
        }

        boolean isRep = representativeRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(r -> analystId.equals(r.getIdentityUserId()));
        dutiesEnforcer.requireEligible(analystId, org.getCreatedByUserId(), c, isRep);

        decisionService.recordDecision(caseId, KycDecisionType.REJECTED,
                request.reasonCode(), request.reason(), analystId);
        c.reject();
        kycCaseRepository.save(c);
        org.markRejected();
        organizationRepository.save(org);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycRejected(org.getId(), caseId,
                decisionService.latestDecisionId(caseId), correlationId);

        return mapper.toResponse(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<KycCaseResponse> search(String status, UUID assignedReviewerId,
                                                 PageRequest pageRequest) {
        KycCaseStatus parsedStatus = status == null ? null
                : KycCaseStatus.valueOf(status.toUpperCase());
        Page<KycCase> page = kycCaseRepository.search(parsedStatus, assignedReviewerId,
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getPage(), pageRequest.getSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        List<KycCaseResponse> content = page.getContent().stream()
                .map(mapper::toResponse).toList();
        PageMetadata meta = PageMetadata.of(page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.of(content, meta);
    }

    // ------------------------------------------------------------------ helpers

    private KycCase requireCase(UUID caseId) {
        return kycCaseRepository.findById(caseId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycCase", caseId));
    }

    private Organization requireOrg(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", organizationId));
    }

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

    private void requireMemberAccessOrAnalyst(Organization org, UUID userId) {
        if (org.getCreatedByUserId().equals(userId)) return;
        boolean isMember = representativeRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(r -> userId.equals(r.getIdentityUserId()));
        if (isMember) return;
        // Analyst access is granted by @RequirePermission at the controller
        // level, not by resource ownership. If the caller reached this method
        // without a member match, the controller must have validated the
        // KYC permission — trust that here.
    }

    private static String generateCaseNumber() {
        return "KYC-" + System.currentTimeMillis() + "-"
                + UuidV7Generator.next().toString().substring(0, 6).toUpperCase();
    }
}