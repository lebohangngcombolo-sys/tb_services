package com.tradebridge.customer.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.customer.api.dto.response.KycDecisionResponse;
import com.tradebridge.customer.service.KycDecisionService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * KYC decision history (spec §6.1F). Read-only — decisions are created
 * by approve/reject endpoints in KycCaseController and are append-only.
 */
@RestController
@RequestMapping("/api/v1/kyc/cases/{caseId}/decisions")
@Tag(name = "KYC Decisions", description = "Decision history")
public class KycDecisionController {

    private final KycDecisionService decisionService;

    public KycDecisionController(KycDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @GetMapping
    @Operation(summary = "List KYC decisions")
    @RequirePermission(PermissionCode.KYC_DECISION_VIEW)
    public ApiResponse<List<KycDecisionResponse>> list(@PathVariable UUID caseId) {
        return ResponseFactory.ok(decisionService.listForCase(caseId));
    }
}