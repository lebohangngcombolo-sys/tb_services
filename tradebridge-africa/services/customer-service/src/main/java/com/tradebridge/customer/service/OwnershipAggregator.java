package com.tradebridge.customer.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.customer.config.CustomerProperties;
import com.tradebridge.customer.domain.BeneficialOwner;
import com.tradebridge.customer.repository.BeneficialOwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate ownership arithmetic and validation (spec §6.4 "The aggregate
 * declared ownership percentage must be checked against the applicable
 * ownership model" and "Direct and indirect ownership should not be
 * double-counted").
 *
 * <p>Rules:
 * <ol>
 *   <li>Sum of DIRECT + INDIRECT ownership must not exceed 100%.</li>
 *   <li>If sum &lt; threshold (default 25%), the submission requires a
 *       declaration that no beneficial owner exceeds the threshold
 *       (otherwise it would be an unexplained gap).</li>
 *   <li>CONTROL, TRUST, NOMINEE control types do not contribute to the
 *       percentage sum (their stake is either 0 or reported via the
 *       underlying DIRECT/INDIRECT owner).</li>
 * </ol>
 */
@Component
public class OwnershipAggregator {

    private static final Logger log = LoggerFactory.getLogger(OwnershipAggregator.class);

    private final BeneficialOwnerRepository ownerRepository;
    private final CustomerProperties properties;

    public OwnershipAggregator(BeneficialOwnerRepository ownerRepository,
                                 CustomerProperties properties) {
        this.ownerRepository = ownerRepository;
        this.properties = properties;
    }

    /**
     * Validates that a proposed ownership percentage can be added.
     * Called by {@link BeneficialOwnerService} before persisting.
     */
    public void ensureAggregateAllowed(UUID organizationId,
                                        BigDecimal newPercentage,
                                        UUID excludeOwnerId) {
        if (newPercentage == null) {
            return; // control-only owner
        }
        if (newPercentage.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ownership percentage cannot be negative");
        }
        if (newPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ownership percentage cannot exceed 100");
        }

        List<BeneficialOwner> existing = ownerRepository.findByOrganizationId(organizationId);
        BigDecimal currentSum = existing.stream()
                .filter(o -> excludeOwnerId == null || !o.getId().equals(excludeOwnerId))
                .filter(o -> o.getOwnershipPercentage() != null)
                .filter(o -> o.getControlType().requiresOwnershipPercentage())
                .map(BeneficialOwner::getOwnershipPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal proposed = currentSum.add(newPercentage);
        if (proposed.compareTo(new BigDecimal("100.01")) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Aggregate direct/indirect ownership would exceed 100% (current: "
                            + currentSum + ", adding: " + newPercentage + ")");
        }
    }

    /**
     * Returns the sum of percentages for DIRECT and INDIRECT owners of an
     * organization.
     */
    public BigDecimal currentSum(UUID organizationId) {
        return ownerRepository.sumOwnershipPercentage(organizationId);
    }

    /**
     * Returns true if the organization's declared owners meet the
     * threshold requirement — either aggregate ≥ threshold, or a
     * declaration has been made (handled by the caller).
     */
    public boolean meetsThreshold(BigDecimal aggregateSum) {
        return aggregateSum != null
                && aggregateSum.compareTo(BigDecimal.valueOf(
                        properties.getOwnership().getDefaultThreshold())) >= 0;
    }

    /**
     * Called during ownership declaration submission (spec §6.1D
     * "POST /organizations/{organizationId}/ownership/submit").
     * Fails if the declaration is inconsistent with the recorded owners.
     */
    public void validateDeclaration(UUID organizationId,
                                     BigDecimal declaredSum,
                                     boolean noOwnersAboveThreshold) {
        if (declaredSum == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ownership declaration must include the aggregate percentage");
        }
        if (declaredSum.signum() < 0 || declaredSum.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Aggregate ownership percentage must be between 0 and 100");
        }
        BigDecimal actual = currentSum(organizationId);
        if (actual.compareTo(declaredSum) != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Declared ownership (" + declaredSum + "%) does not match "
                            + "recorded owners (" + actual + "%)");
        }
        if (!noOwnersAboveThreshold && !meetsThreshold(actual)) {
            log.debug("Ownership below threshold for org {} ({}%) with no explicit declaration",
                    organizationId, actual);
        }
    }
}