package com.tradebridge.country.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.common.response.FieldError;
import com.tradebridge.country.api.dto.request.KycRequirementRequest;
import com.tradebridge.country.api.dto.response.KycRequirementResponse;
import com.tradebridge.country.domain.Country;
import com.tradebridge.country.domain.KycRequirement;
import com.tradebridge.country.event.CountryEventPublisher;
import com.tradebridge.country.repository.CountryRepository;
import com.tradebridge.country.repository.KycRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * KYC requirement rules (spec §4.1D, §4.4).
 *
 * <p>Validation of file metadata (MIME, size) follows spec §4.4
 * "File requirements must specify sensible MIME-type and size restrictions".
 * Defaults come from `FileMetadataValidator` if the caller omits them.
 */
@Service
public class KycRequirementService {

    private static final long MAX_FILE_SIZE_DEFAULT = 10L * 1024 * 1024; // 10 MiB

    private final KycRequirementRepository repository;
    private final CountryRepository countryRepository;
    private final CountryEventPublisher eventPublisher;

    public KycRequirementService(KycRequirementRepository repository,
                                  CountryRepository countryRepository,
                                  CountryEventPublisher eventPublisher) {
        this.repository = repository;
        this.countryRepository = countryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<KycRequirementResponse> listActive(String countryCode) {
        String iso2 = requireCountry(countryCode).getIso2Code();
        return repository.findByCountryCodeAndEnabledTrue(iso2).stream()
                .map(KycRequirementService::toResponse)
                .toList();
    }

    @Transactional
    public KycRequirementResponse create(String countryCode,
                                          KycRequirementRequest request,
                                          UUID actorId) {
        Country country = requireCountry(countryCode);
        if (repository.existsByCountryCodeAndCustomerTypeAndRequirementCode(
                country.getIso2Code(), request.customerType(), request.requirementCode())) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "Requirement code already exists for this customer type: "
                            + request.requirementCode());
        }
        validateFileRules(request);

        KycRequirement requirement = new KycRequirement(
                UuidV7Generator.next(),
                country.getIso2Code(),
                request.customerType(),
                request.documentType(),
                request.requirementCode(),
                request.description(),
                request.required(),
                request.allowedMimeTypes() == null || request.allowedMimeTypes().isEmpty()
                        ? List.of("application/pdf", "image/jpeg", "image/png")
                        : request.allowedMimeTypes(),
                request.maxFileSizeBytes() == null ? MAX_FILE_SIZE_DEFAULT : request.maxFileSizeBytes(),
                request.validityDays(),
                request.enabled());
        repository.save(requirement);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycRequirementUpdated(
                country.getIso2Code(), requirement.getRequirementCode(), correlationId);

        return toResponse(requirement);
    }

    @Transactional
    public KycRequirementResponse update(UUID requirementId,
                                          KycRequirementRequest request,
                                          UUID actorId) {
        KycRequirement requirement = repository.findById(requirementId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycRequirement", requirementId));
        validateFileRules(request);

        requirement.updateDetails(request.description(), request.required(),
                request.allowedMimeTypes(), request.maxFileSizeBytes(), request.validityDays());
        requirement.setEnabled(request.enabled());
        repository.save(requirement);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycRequirementUpdated(
                requirement.getCountryCode(), requirement.getRequirementCode(), correlationId);

        return toResponse(requirement);
    }

    @Transactional
    public KycRequirementResponse changeStatus(UUID requirementId, boolean enabled, UUID actorId) {
        KycRequirement requirement = repository.findById(requirementId)
                .orElseThrow(() -> ResourceNotFoundException.of("KycRequirement", requirementId));
        requirement.setEnabled(enabled);
        repository.save(requirement);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishKycRequirementUpdated(
                requirement.getCountryCode(), requirement.getRequirementCode(), correlationId);

        return toResponse(requirement);
    }

    private static void validateFileRules(KycRequirementRequest request) {
        if (request.maxFileSizeBytes() != null && request.maxFileSizeBytes() <= 0) {
            throw new ValidationException("maxFileSizeBytes must be positive",
                    List.of(new FieldError("maxFileSizeBytes", "Must be positive")));
        }
        if (request.maxFileSizeBytes() != null && request.maxFileSizeBytes() > 50L * 1024 * 1024) {
            throw new ValidationException("maxFileSizeBytes is too large (max 50 MiB)",
                    List.of(new FieldError("maxFileSizeBytes", "Max 50 MiB")));
        }
        if (request.validityDays() != null && request.validityDays() <= 0) {
            throw new ValidationException("validityDays must be positive",
                    List.of(new FieldError("validityDays", "Must be positive")));
        }
    }

    private Country requireCountry(String countryCode) {
        return countryRepository.findByIso2Code(countryCode.toUpperCase())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", countryCode));
    }

    static KycRequirementResponse toResponse(KycRequirement r) {
        return new KycRequirementResponse(
                r.getId(), r.getCountryCode(), r.getCustomerType(), r.getDocumentType(),
                r.getRequirementCode(), r.getDescription(), r.isRequired(),
                r.getAllowedMimeTypes(), r.getMaxFileSizeBytes(), r.getValidityDays(),
                r.isEnabled());
    }
}