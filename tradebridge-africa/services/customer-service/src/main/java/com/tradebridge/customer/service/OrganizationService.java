package com.tradebridge.customer.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.customer.api.dto.request.CreateOrganizationRequest;
import com.tradebridge.customer.api.dto.request.UpdateOrganizationRequest;
import com.tradebridge.customer.api.dto.response.OrganizationResponse;
import com.tradebridge.customer.api.dto.response.OrganizationStatusResponse;
import com.tradebridge.customer.client.CountryServiceClient;
import com.tradebridge.customer.domain.Organization;
import com.tradebridge.customer.domain.OrganizationRepresentative;
import com.tradebridge.customer.domain.enums.KycCaseStatus;
import com.tradebridge.customer.domain.enums.RepresentativeType;
import com.tradebridge.customer.domain.enums.RepresentativeVerificationStatus;
import com.tradebridge.customer.event.CustomerEventPublisher;
import com.tradebridge.customer.mapper.OrganizationMapper;
import com.tradebridge.customer.repository.KycCaseRepository;
import com.tradebridge.customer.repository.OrganizationRepository;
import com.tradebridge.customer.repository.OrganizationRepresentativeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Organization lifecycle (spec §6.1B).
 *
 * <p>Access checks: only the creator or an active representative can read
 * or modify. Staff (KYC_ANALYST, ADMIN) bypass via permission checks on
 * the controller, not here — this method is called by both and the caller
 * decides what's allowed.
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRepresentativeRepository representativeRepository;
    private final KycCaseRepository kycCaseRepository;
    private final CountryServiceClient countryServiceClient;
    private final CustomerEventPublisher eventPublisher;
    private final OrganizationMapper mapper;

    public OrganizationService(OrganizationRepository organizationRepository,
                                OrganizationRepresentativeRepository representativeRepository,
                                KycCaseRepository kycCaseRepository,
                                CountryServiceClient countryServiceClient,
                                CustomerEventPublisher eventPublisher,
                                OrganizationMapper mapper) {
        this.organizationRepository = organizationRepository;
        this.representativeRepository = representativeRepository;
        this.kycCaseRepository = kycCaseRepository;
        this.countryServiceClient = countryServiceClient;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Transactional
    public OrganizationResponse create(UUID userId, CreateOrganizationRequest request) {
        // Verify country is enabled by fetching its configuration (fails
        // closed if unreachable)
        countryServiceClient.getActiveConfiguration(request.countryCode());

        // Check for duplicate registration within (country, authority, number)
        String authority = request.registrationAuthority() == null ? "" : request.registrationAuthority();
        Optional<Organization> existing = organizationRepository
                .findDuplicate(request.countryCode().toUpperCase(), authority,
                        request.registrationNumber());
        if (existing.isPresent()) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "An organization with this registration number already exists");
        }

        Organization org = new Organization(
                UuidV7Generator.next(),
                request.legalName(),
                request.organizationType(),
                request.registrationNumber(),
                request.countryCode(),
                userId);
        org.updateDetails(request.legalName(), request.tradingName(), request.taxNumber(),
                request.incorporationDate(), request.registrationAuthority(),
                request.businessDescription(), request.website());
        org.assignOrganizationNumber(generateOrganizationNumber());
        organizationRepository.save(org);

        // The creating user becomes the initial authorized signatory
        representativeRepository.save(new OrganizationRepresentative(
                UuidV7Generator.next(), org.getId(), userId,
                "Representative", "Of Organization",
                // We cannot query the user's email from Identity here without
                // a service token — the field is required by schema so we
                // use a placeholder. Service can be enriched later when the
                // identity lookup is wired.
                "signatory+" + userId + "@pending.tradebridge.africa",
                null, RepresentativeType.SIGNATORY, true));

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishOrganizationCreated(org.getId(), org.getCountryCode(), correlationId);

        return mapper.toResponse(org);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listForUser(UUID userId) {
        return organizationRepository.findAccessibleByUser(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID organizationId, UUID userId) {
        Organization org = requireAccess(organizationId, userId);
        return mapper.toResponse(org);
    }

    @Transactional
    public OrganizationResponse update(UUID organizationId, UUID userId,
                                        UpdateOrganizationRequest request) {
        Organization org = requireAccess(organizationId, userId);
        if (!org.canBeEdited()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Organization cannot be edited in status " + org.getStatus());
        }
        org.updateDetails(request.legalName(), request.tradingName(), request.taxNumber(),
                request.incorporationDate(), request.registrationAuthority(),
                request.businessDescription(), request.website());
        organizationRepository.save(org);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishOrganizationUpdated(org.getId(),
                "details", correlationId);

        return mapper.toResponse(org);
    }

    @Transactional(readOnly = true)
    public OrganizationStatusResponse status(UUID organizationId, UUID userId) {
        Organization org = requireAccess(organizationId, userId);
        var activeCase = kycCaseRepository
                .findFirstByOrganizationIdAndStatusInOrderByCreatedAtDesc(
                        organizationId,
                        List.of(KycCaseStatus.SUBMITTED, KycCaseStatus.IN_REVIEW,
                                KycCaseStatus.INFORMATION_REQUESTED, KycCaseStatus.RESUBMITTED));
        return new OrganizationStatusResponse(
                org.getId(),
                org.getStatus().name(),
                activeCase.map(c -> c.getId()).orElse(null),
                activeCase.map(c -> c.getStatus().name()).orElse(null),
                org.getUpdatedAt());
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Ensures the user is either the organization's creator or an active
     * representative. Does NOT check staff permissions — that's done at
     * the controller level via {@code @RequirePermission}.
     */
    Organization requireAccess(UUID organizationId, UUID userId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", organizationId));
        if (org.getCreatedByUserId().equals(userId)) {
            return org;
        }
        boolean isRep = representativeRepository.findByOrganizationId(organizationId).stream()
                .anyMatch(r -> userId.equals(r.getIdentityUserId())
                        && r.getVerificationStatus() != RepresentativeVerificationStatus.REJECTED);
        if (isRep) {
            return org;
        }
        throw new ForbiddenException(ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                "User is not authorized for this organization");
    }

    private static String generateOrganizationNumber() {
        return "ORG-" + System.currentTimeMillis() + "-"
                + UuidV7Generator.next().toString().substring(0, 6).toUpperCase();
    }
}