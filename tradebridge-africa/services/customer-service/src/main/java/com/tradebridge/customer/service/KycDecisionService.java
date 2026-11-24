package com.tradebridge.customer.service;

import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.customer.api.dto.response.KycDecisionResponse;
import com.tradebridge.customer.domain.KycDecision;
import com.tradebridge.customer.domain.enums.KycDecisionType;
import com.tradebridge.customer.mapper.KycCaseMapper;
import com.tradebridge.customer.repository.KycDecisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * KYC decisions (spec §6.2, §6.4). Append-only, versioned, immutable.
 */
@Service
public class KycDecisionService {

    private final KycDecisionRepository decisionRepository;
    private final KycCaseMapper mapper;

    public KycDecisionService(KycDecisionRepository decisionRepository,
                                KycCaseMapper mapper) {
        this.decisionRepository = decisionRepository;
        this.mapper = mapper;
    }

    /**
     * Append a new decision. Never modifies existing decisions.
     * Returns the newly created decision ID.
     */
    @Transactional
    public UUID recordDecision(UUID caseId, KycDecisionType decision,
                                String reasonCode, String reason, UUID decidedBy) {
        int nextVersion = decisionRepository.findMaxDecisionVersion(caseId) + 1;
        UUID supersedes = decisionRepository.findFirstByKycCaseIdOrderByDecisionVersionDesc(caseId)
                .map(KycDecision::getId).orElse(null);

        KycDecision entity = new KycDecision(
                UuidV7Generator.next(),
                caseId,
                decision,
                reasonCode,
                reason,
                decidedBy,
                nextVersion,
                supersedes);
        decisionRepository.save(entity);
        return entity.getId();
    }

    @Transactional(readOnly = true)
    public List<KycDecisionResponse> listForCase(UUID caseId) {
        return decisionRepository.findByKycCaseIdOrderByDecisionVersionDesc(caseId).stream()
                .map(mapper::toDecisionResponse).toList();
    }

    @Transactional(readOnly = true)
    public UUID latestDecisionId(UUID caseId) {
        return decisionRepository.findFirstByKycCaseIdOrderByDecisionVersionDesc(caseId)
                .map(KycDecision::getId)
                .orElse(null);
    }
}