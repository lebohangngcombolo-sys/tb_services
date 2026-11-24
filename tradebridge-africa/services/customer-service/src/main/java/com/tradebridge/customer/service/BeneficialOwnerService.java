package com.tradebridge.customer.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.customer.api.dto.request.AddBeneficialOwnerRequest;
import com.tradebridge.customer.api.dto.request.OwnershipDeclarationRequest;
import com.tradebridge.customer.api.dto.response.BeneficialOwnerResponse;
import com.tradebridge.customer.domain.BeneficialOwner;
import com.tradebridge.customer.domain.Organization;
import com.tradebridge.customer.domain.enums.ControlType;
import com.tradebridge.customer.event.CustomerEventPublisher;
import com.tradebridge.customer.mapper.BeneficialOwnerMapper;
import com.tradebridge.customer.repository.BeneficialOwnerRepository;
import com.tradebridge.customer.repository.OrganizationRepository;
import com.tradebridge.customer.repository.OrganizationRepresentativeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Beneficial ownership (spec §6.1D, §6.4).
 */
@Service
public class BeneficialOwnerService {

    private final BeneficialOwnerRepository ownerRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationRepresentativeRepository representativeRepository;
    private final OwnershipAggregator ownershipAggregator;
    private final CustomerEventPublisher eventPublisher;
    private final BeneficialOwnerMapper mapper;

    public BeneficialOwnerService(BeneficialOwnerRepository ownerRepository,
                                    OrganizationRepository organizationRepository,
                                    OrganizationRepresentativeRepository representativeRepository,
                                    OwnershipAggregator ownershipAggregator,
                                    CustomerEventPublisher eventPublisher,
                                    BeneficialOwnerMapper mapper) {
        this.ownerRepository = ownerRepository;
        this.organizationRepository = organizationRepository;
        this.representativeRepository = representativeRepository;
        this.ownershipAggregator = ownershipAggregator;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Transactional
    public BeneficialOwnerResponse add(UUID organizationId, UUID userId,
                                        AddBeneficialOwnerRequest request) {
        Organization org = requireOrg(organizationId);
        requireRepresentativeAccess(org, userId);

        // Validate aggregate ownership if a percentage is provided
        ControlType controlType = ControlType.valueOf(request.controlType());
        if (request.ownershipPercentage() != null) {
            ownershipAggregator.ensureAggregateAllowed(organizationId,
                    request.ownershipPercentage(), null);
        }
        if (controlType.requiresOwnershipPercentage() && request.ownershipPercentage() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Control type " + controlType + " requires an ownership percentage");
        }

        BeneficialOwner owner = new BeneficialOwner(
                UuidV7Generator.next(),
                organizationId,
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.nationalityCountryCode(),
                request.residenceCountryCode(),
                request.ownershipPercentage(),
                controlType,
                request.identityReference() == null ? null
                        : sha256Hash(request.identityReference()));
        ownerRepository.save(owner);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishBeneficialOwnerDeclared(organizationId, owner.getId(), correlationId);

        return mapper.toResponse(owner);
    }

    @Transactional(readOnly = true)
    public List<BeneficialOwnerResponse> list(UUID organizationId, UUID userId) {
        Organization org = requireOrg(organizationId);
        requireMemberAccess(org, userId);
        return ownerRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BeneficialOwnerResponse get(UUID organizationId, UUID ownerId, UUID userId) {
        Organization org = requireOrg(organizationId);
        requireMemberAccess(org, userId);
        BeneficialOwner owner = ownerRepository.findByIdAndOrganizationId(ownerId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("BeneficialOwner", ownerId));
        return mapper.toResponse(owner);
    }

    @Transactional
    public BeneficialOwnerResponse update(UUID organizationId, UUID ownerId, UUID userId,
                                            AddBeneficialOwnerRequest request) {
        Organization org = requireOrg(organizationId);
        requireRepresentativeAccess(org, userId);

        BeneficialOwner owner = ownerRepository.findByIdAndOrganizationId(ownerId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("BeneficialOwner", ownerId));

        if (request.ownershipPercentage() != null) {
            ownershipAggregator.ensureAggregateAllowed(organizationId,
                    request.ownershipPercentage(), ownerId);
        }

        ControlType controlType = request.controlType() == null ? null
                : ControlType.valueOf(request.controlType());
        owner.updateDetails(request.firstName(), request.lastName(), request.dateOfBirth(),
                request.nationalityCountryCode(), request.residenceCountryCode(),
                request.ownershipPercentage(), controlType,
                request.identityReference() == null ? null
                        : sha256Hash(request.identityReference()));
        ownerRepository.save(owner);
        return mapper.toResponse(owner);
    }

    @Transactional
    public void remove(UUID organizationId, UUID ownerId, UUID userId) {
        Organization org = requireOrg(organizationId);
        requireRepresentativeAccess(org, userId);
        BeneficialOwner owner = ownerRepository.findByIdAndOrganizationId(ownerId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("BeneficialOwner", ownerId));
        ownerRepository.delete(owner);
    }

    @Transactional
    public void submitDeclaration(UUID organizationId, UUID userId,
                                   OwnershipDeclarationRequest request) {
        Organization org = requireOrg(organizationId);
        requireRepresentativeAccess(org, userId);

        BigDecimal currentSum = ownershipAggregator.currentSum(organizationId);
        ownershipAggregator.validateDeclaration(organizationId, currentSum,
                request.noBeneficialOwnersAboveThreshold());

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishOwnershipDeclarationSubmitted(organizationId, correlationId);
    }

    // ------------------------------------------------------------------ helpers

    private Organization requireOrg(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", organizationId));
    }

    private void requireRepresentativeAccess(Organization org, UUID userId) {
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

    private static String sha256Hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}