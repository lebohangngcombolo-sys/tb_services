package com.tradebridge.customer.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.customer.api.dto.request.AddRepresentativeRequest;
import com.tradebridge.customer.api.dto.request.VerifyRepresentativeRequest;
import com.tradebridge.customer.api.dto.response.RepresentativeResponse;
import com.tradebridge.customer.domain.Organization;
import com.tradebridge.customer.domain.OrganizationRepresentative;
import com.tradebridge.customer.domain.enums.RepresentativeType;
import com.tradebridge.customer.event.CustomerEventPublisher;
import com.tradebridge.customer.mapper.RepresentativeMapper;
import com.tradebridge.customer.repository.OrganizationRepository;
import com.tradebridge.customer.repository.OrganizationRepresentativeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Organization representatives (spec §6.1C).
 */
@Service
public class RepresentativeService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationRepresentativeRepository representativeRepository;
    private final CustomerEventPublisher eventPublisher;
    private final RepresentativeMapper mapper;

    public RepresentativeService(OrganizationRepository organizationRepository,
                                  OrganizationRepresentativeRepository representativeRepository,
                                  CustomerEventPublisher eventPublisher,
                                  RepresentativeMapper mapper) {
        this.organizationRepository = organizationRepository;
        this.representativeRepository = representativeRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Transactional
    public RepresentativeResponse add(UUID organizationId, UUID userId,
                                       AddRepresentativeRequest request) {
        Organization org = requireOrg(organizationId);
        requireAuthorizedRepresentative(org, userId);

        if (representativeRepository.existsByOrganizationIdAndEmail(
                organizationId, request.email().toLowerCase())) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "A representative with this email already exists for the organization");
        }

        RepresentativeType type = RepresentativeType.valueOf(request.representativeType());
        OrganizationRepresentative rep = new OrganizationRepresentative(
                UuidV7Generator.next(),
                organizationId,
                request.identityUserId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                type,
                request.authorizedSignatory());
        representativeRepository.save(rep);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishRepresentativeAdded(organizationId, rep.getId(), correlationId);

        return mapper.toResponse(rep);
    }

    @Transactional(readOnly = true)
    public List<RepresentativeResponse> list(UUID organizationId, UUID userId) {
        Organization org = requireOrg(organizationId);
        requireReadAccess(org, userId);
        return representativeRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public RepresentativeResponse update(UUID organizationId, UUID representativeId,
                                          UUID userId, AddRepresentativeRequest request) {
        Organization org = requireOrg(organizationId);
        requireAuthorizedRepresentative(org, userId);

        OrganizationRepresentative rep = representativeRepository
                .findByIdAndOrganizationId(representativeId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "OrganizationRepresentative", representativeId));

        rep.updateDetails(request.firstName(), request.lastName(), request.email(),
                request.phoneNumber(),
                request.representativeType() == null ? null
                        : RepresentativeType.valueOf(request.representativeType()),
                request.authorizedSignatory());
        representativeRepository.save(rep);
        return mapper.toResponse(rep);
    }

    @Transactional
    public void remove(UUID organizationId, UUID representativeId, UUID userId) {
        Organization org = requireOrg(organizationId);
        requireAuthorizedRepresentative(org, userId);

        OrganizationRepresentative rep = representativeRepository
                .findByIdAndOrganizationId(representativeId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "OrganizationRepresentative", representativeId));

        // Ensure at least one authorized signatory remains
        if (rep.isAuthorizedSignatory()
                && representativeRepository.countAuthorizedSignatories(organizationId) <= 1) {
            throw new com.tradebridge.common.exception.BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot remove the last authorized signatory");
        }
        rep.revokeAuthorization();
        representativeRepository.delete(rep);
    }

    @Transactional
    public RepresentativeResponse verify(UUID organizationId, UUID representativeId,
                                          VerifyRepresentativeRequest request, UUID actorId) {
        OrganizationRepresentative rep = representativeRepository
                .findByIdAndOrganizationId(representativeId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "OrganizationRepresentative", representativeId));

        if ("VERIFIED".equalsIgnoreCase(request.status())) {
            rep.verify();
        } else {
            rep.reject();
        }
        representativeRepository.save(rep);
        return mapper.toResponse(rep);
    }

    // ------------------------------------------------------------------ helpers

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

    private void requireReadAccess(Organization org, UUID userId) {
        if (org.getCreatedByUserId().equals(userId)) return;
        boolean isRep = representativeRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(r -> userId.equals(r.getIdentityUserId()));
        if (!isRep) {
            throw new ForbiddenException(ErrorCode.RESOURCE_OWNERSHIP_VIOLATION,
                    "User is not a member of this organization");
        }
    }
}