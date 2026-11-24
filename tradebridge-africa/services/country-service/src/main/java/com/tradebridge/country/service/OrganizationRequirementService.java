package com.tradebridge.country.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.country.api.dto.request.OrganizationRequirementRequest;
import com.tradebridge.country.api.dto.response.OrganizationRequirementResponse;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.OrganizationRequirement;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.CountryRepository;
import com.tradebridge.country.repository.OrganizationRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Organization requirement rules (spec §4.1C, §4.4).
 */
@Service
public class OrganizationRequirementService {

    private final OrganizationRequirementRepository repository;
    private final CountryRepository countryRepository;
    private final CountryEventPublisher eventPublisher;

    public OrganizationRequirementService(OrganizationRequirementRepository repository,
                                           CountryRepository countryRepository,
                                           CountryEventPublisher eventPublisher) {
        this.repository = repository;
        this.countryRepository = countryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<OrganizationRequirementResponse> listActive(String countryCode) {
        String iso2 = requireCountry(countryCode).getIso2Code();
        return repository.findActiveByCountry(iso2, Instant.now()).stream()
                .map(OrganizationRequirementService::toResponse)
                .toList();
    }

    @Transactional
    public OrganizationRequirementResponse create(String countryCode,
                                                   OrganizationRequirementRequest request,
                                                   UUID actorId) {
        Country country = requireCountry(countryCode);
        if (repository.existsByCountryCodeAndOrganizationTypeAndRequirementCode(
                country.getIso2Code(), request.organizationType(), request.requirementCode())) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "Requirement code already exists for this organization type: "
                            + request.requirementCode());
        }
        validateEffectiveWindow(request.effectiveFrom(), request.effectiveUntil());

        OrganizationRequirement requirement = new OrganizationRequirement(
                UuidV7Generator.next(),
                country.getIso2Code(),
                request.organizationType(),
                request.requirementCode(),
                request.description(),
                request.required(),
                request.validationRule(),
                request.effectiveFrom(),
                request.effectiveUntil(),
                request.enabled());
        repository.save(requirement);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishOrganizationRequirementUpdated(
                country.getIso2Code(), requirement.getRequirementCode(), correlationId);

        return toResponse(requirement);
    }

    @Transactional
    public OrganizationRequirementResponse update(UUID requirementId,
                                                   OrganizationRequirementRequest request,
                                                   UUID actorId) {
        OrganizationRequirement requirement = repository.findById(requirementId)
                .orElseThrow(() -> ResourceNotFoundException.of("OrganizationRequirement", requirementId));
        validateEffectiveWindow(request.effectiveFrom(), request.effectiveUntil());

        requirement.updateDetails(request.description(), request.required(),
                request.validationRule(), request.effectiveFrom(), request.effectiveUntil());
        requirement.setEnabled(request.enabled());
        repository.save(requirement);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishOrganizationRequirementUpdated(
                requirement.getCountryCode(), requirement.getRequirementCode(), correlationId);

        return toResponse(requirement);
    }

    @Transactional
    public OrganizationRequirementResponse changeStatus(UUID requirementId, boolean enabled, UUID actorId) {
        OrganizationRequirement requirement = repository.findById(requirementId)
                .orElseThrow(() -> ResourceNotFoundException.of("OrganizationRequirement", requirementId));
        requirement.setEnabled(enabled);
        repository.save(requirement);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishOrganizationRequirementUpdated(
                requirement.getCountryCode(), requirement.getRequirementCode(), correlationId);

        return toResponse(requirement);
    }

    private static void validateEffectiveWindow(Instant from, Instant until) {
        if (from != null && until != null && !until.isAfter(from)) {
            throw new com.tradebridge.common.exception.ValidationException(
                    "effectiveUntil must be after effectiveFrom",
                    List.of(new com.tradebridge.common.response.FieldError(
                            "effectiveUntil", "Must be after effectiveFrom")));
        }
    }

    private Country requireCountry(String countryCode) {
        return countryRepository.findByIso2Code(countryCode.toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", countryCode));
    }

    static OrganizationRequirementResponse toResponse(OrganizationRequirement r) {
        return new OrganizationRequirementResponse(
                r.getId(), r.getCountryCode(), r.getOrganizationType(),
                r.getRequirementCode(), r.getDescription(), r.isRequired(),
                r.getValidationRule(), r.getEffectiveFrom(), r.getEffectiveUntil(),
                r.isEnabled());
    }
}