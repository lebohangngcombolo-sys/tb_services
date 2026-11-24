package com.tradebridge.customer.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.common.pagination.PageMetadata;
import com.tradebridge.common.pagination.PageRequest;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.customer.api.dto.request.CreateCustomerProfileRequest;
import com.tradebridge.customer.api.dto.request.UpdateCustomerProfileRequest;
import com.tradebridge.customer.api.dto.request.UpdateCustomerStatusRequest;
import com.tradebridge.customer.api.dto.response.CustomerProfileResponse;
import com.tradebridge.customer.domain.CustomerProfile;
import com.tradebridge.customer.domain.CustomerStatusHistory;
import com.tradebridge.customer.domain.enums.CustomerStatus;
import com.tradebridge.customer.event.CustomerEventPublisher;
import com.tradebridge.customer.mapper.CustomerProfileMapper;
import com.tradebridge.customer.repository.CustomerProfileRepository;
import com.tradebridge.customer.repository.CustomerStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer profile lifecycle (spec §6.1A).
 */
@Service
public class CustomerProfileService {

    private final CustomerProfileRepository profileRepository;
    private final CustomerStatusHistoryRepository historyRepository;
    private final CustomerEventPublisher eventPublisher;
    private final CustomerProfileMapper mapper;

    public CustomerProfileService(CustomerProfileRepository profileRepository,
                                    CustomerStatusHistoryRepository historyRepository,
                                    CustomerEventPublisher eventPublisher,
                                    CustomerProfileMapper mapper) {
        this.profileRepository = profileRepository;
        this.historyRepository = historyRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Transactional
    public CustomerProfileResponse create(UUID identityUserId, CreateCustomerProfileRequest request) {
        if (profileRepository.existsByIdentityUserId(identityUserId)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "A customer profile already exists for this user");
        }
        CustomerProfile profile = new CustomerProfile(
                UuidV7Generator.next(),
                identityUserId,
                request.customerType(),
                request.countryCode(),
                request.preferredLanguage());
        profile.assignCustomerNumber(generateCustomerNumber());
        profile.activate();
        profileRepository.save(profile);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishProfileCreated(profile.getId(), identityUserId,
                profile.getCountryCode(), correlationId);

        return mapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getOwn(UUID identityUserId) {
        return profileRepository.findByIdentityUserId(identityUserId)
                .map(mapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerProfile", identityUserId));
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse get(UUID customerId) {
        return profileRepository.findById(customerId)
                .map(mapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerProfile", customerId));
    }

    @Transactional
    public CustomerProfileResponse updateOwn(UUID identityUserId, UpdateCustomerProfileRequest request) {
        CustomerProfile profile = profileRepository.findByIdentityUserId(identityUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerProfile", identityUserId));
        profile.updatePreferences(request.preferredLanguage());
        profileRepository.save(profile);
        return mapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerProfileResponse> search(String query, String countryCode,
                                                          String status, PageRequest pageRequest) {
        CustomerStatus parsedStatus = status == null ? null : CustomerStatus.valueOf(status.toUpperCase());
        Page<CustomerProfile> page = profileRepository.search(
                query, countryCode == null ? null : countryCode.toUpperCase(),
                parsedStatus,
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getPage(), pageRequest.getSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        List<CustomerProfileResponse> content = page.getContent().stream()
                .map(mapper::toResponse).toList();
        PageMetadata meta = PageMetadata.of(page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.of(content, meta);
    }

    @Transactional
    public CustomerProfileResponse updateStatus(UUID customerId,
                                                 UpdateCustomerStatusRequest request,
                                                 UUID actorId) {
        CustomerProfile profile = profileRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerProfile", customerId));
        CustomerStatus oldStatus = profile.getStatus();
        CustomerStatus newStatus = CustomerStatus.valueOf(request.status().toUpperCase());
        switch (newStatus) {
            case ACTIVE -> profile.activate();
            case SUSPENDED -> profile.suspend();
            case CLOSED -> profile.close();
            default -> { }
        }
        profileRepository.save(profile);

        if (oldStatus != newStatus) {
            historyRepository.save(new CustomerStatusHistory(
                    UuidV7Generator.next(), customerId, null,
                    oldStatus.name(), newStatus.name(), request.reason(), actorId));
        }
        return mapper.toResponse(profile);
    }

    /**
     * Lookup used internally by other services when verifying that a
     * customer exists.
     */
    @Transactional(readOnly = true)
    public CustomerProfile getRequiredByIdentityUserId(UUID identityUserId) {
        return profileRepository.findByIdentityUserId(identityUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerProfile", identityUserId));
    }

    private static String generateCustomerNumber() {
        return "CUS-" + System.currentTimeMillis() + "-"
                + UuidV7Generator.next().toString().substring(0, 6).toUpperCase();
    }
}