package com.tradebridge.customer.mapper;

import com.tradebridge.customer.api.dto.response.KycDecisionResponse;
import com.tradebridge.customer.domain.KycDecision;
import org.springframework.stereotype.Component;

@Component
public class KycDecisionMapper {

    public KycDecisionResponse toResponse(KycDecision decision) {
        if (decision == null) return null;
        return new KycDecisionResponse(
                decision.getId(),
                decision.getKycCaseId(),
                decision.getDecision().name(),
                decision.getDecisionReasonCode(),
                decision.getDecisionReason(),
                decision.getDecidedByUserId(),
                decision.getDecisionVersion(),
                decision.getSupersedesDecisionId(),
                decision.getDecidedAt());
    }
}