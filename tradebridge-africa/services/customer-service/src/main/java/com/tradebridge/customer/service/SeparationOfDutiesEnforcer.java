package com.tradebridge.customer.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.customer.config.CustomerProperties;
import com.tradebridge.customer.domain.KycCase;
import com.tradebridge.customer.repository.KycDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enforces separation of duties (spec §6.4 "A reviewer cannot approve their
 * own organization's application" and "For higher-risk cases, require a
 * second authorized reviewer").
 *
 * <p>Rules enforced:
 * <ol>
 *   <li>The reviewer cannot be a representative of the organization.</li>
 *   <li>The reviewer cannot be the organization's creator.</li>
 *   <li>The reviewer cannot have created a previous APPROVED decision on
 *       this case (for second-reviewer scenarios).</li>
 *   <li>For HIGH or CRITICAL risk, a second, different reviewer must have
 *       completed a review first.</li>
 * </ol>
 */
@Component
public class SeparationOfDutiesEnforcer {

    private static final Logger log = LoggerFactory.getLogger(SeparationOfDutiesEnforcer.class);

    private final KycDecisionRepository decisionRepository;
    private final CustomerProperties properties;

    public SeparationOfDutiesEnforcer(KycDecisionRepository decisionRepository,
                                        CustomerProperties properties) {
        this.decisionRepository = decisionRepository;
        this.properties = properties;
    }

    /**
     * Verifies that {@code reviewerId} is allowed to make a decision on
     * {@code kycCase}. Throws {@link BusinessException} on violation.
     *
     * @param reviewerId       the authenticated reviewer's user ID
     * @param organizationOwnerId the ID of the user who created the organization
     * @param kycCase          the case under review
     * @param isRepresentativeOfOrg true if reviewer is also a representative
     */
    public void requireEligible(UUID reviewerId,
                                 UUID organizationOwnerId,
                                 KycCase kycCase,
                                 boolean isRepresentativeOfOrg) {
        if (reviewerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Reviewer identity missing");
        }
        if (reviewerId.equals(organizationOwnerId)) {
            throw new BusinessException(ErrorCode.SEPARATION_OF_DUTIES_VIOLATION,
                    "A reviewer cannot decide on their own organization's KYC case");
        }
        if (isRepresentativeOfOrg) {
            throw new BusinessException(ErrorCode.SEPARATION_OF_DUTIES_VIOLATION,
                    "A reviewer cannot decide on a case where they are a representative");
        }
        // Second-reviewer requirement for high-risk cases
        if (requiresSecondReviewer(kycCase)) {
            boolean hasPreviousApprovalByOther = decisionRepository
                    .findByKycCaseIdOrderByDecisionVersionDesc(kycCase.getId()).stream()
                    .filter(d -> d.getDecision() == com.tradebridge.customer.domain.enums.KycDecisionType.APPROVED)
                    .anyMatch(d -> !d.getDecidedByUserId().equals(reviewerId));
            if (!hasPreviousApprovalByOther) {
                throw new BusinessException(ErrorCode.SEPARATION_OF_DUTIES_VIOLATION,
                        "High-risk KYC cases require a second authorized reviewer");
            }
        }
    }

    private boolean requiresSecondReviewer(KycCase kycCase) {
        String risk = kycCase.getRiskLevel();
        if (risk == null) return false;
        return properties.getKyc().getSecondReviewerRiskLevels().contains(risk.toUpperCase());
    }
}