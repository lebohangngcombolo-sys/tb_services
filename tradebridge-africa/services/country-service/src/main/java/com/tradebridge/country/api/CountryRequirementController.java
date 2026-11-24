package com.tradebridge.country.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.country.api.dto.response.KycRequirementResponse;
import com.tradebridge.country.api.dto.response.OrganizationRequirementResponse;
import com.tradebridge.country.service.KycRequirementService;
import com.tradebridge.country.service.OrganizationRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public onboarding-requirement reads (spec §4.1A, §4.1C, §4.1D).
 *
 * <p>These are public because customer onboarding UIs must render
 * requirements before the user submits anything.
 */
@RestController
@RequestMapping("/api/v1/countries/{countryCode}")
@Tag(name = "Country Requirements", description = "Public onboarding requirements")
public class CountryRequirementController {

    private final OrganizationRequirementService organizationRequirementService;
    private final KycRequirementService kycRequirementService;

    public CountryRequirementController(OrganizationRequirementService organizationRequirementService,
                                        KycRequirementService kycRequirementService) {
        this.organizationRequirementService = organizationRequirementService;
        this.kycRequirementService = kycRequirementService;
    }

    @GetMapping("/requirements")
    @Operation(summary = "Get all onboarding requirements for a country")
    public ApiResponse<Map<String, Object>> requirements(@PathVariable String countryCode) {
        return ResponseFactory.ok(Map.of(
                "countryCode", countryCode.toUpperCase(),
                "organizationRequirements", organizationRequirementService.listActive(countryCode),
                "kycRequirements", kycRequirementService.listActive(countryCode)));
    }

    @GetMapping("/organization-requirements")
    @Operation(summary = "Get active organization requirements")
    public ApiResponse<List<OrganizationRequirementResponse>> organizationRequirements(
            @PathVariable String countryCode) {
        return ResponseFactory.ok(organizationRequirementService.listActive(countryCode));
    }

    @GetMapping("/kyc-requirements")
    @Operation(summary = "Get active KYC requirements")
    public ApiResponse<List<KycRequirementResponse>> kycRequirements(@PathVariable String countryCode) {
        return ResponseFactory.ok(kycRequirementService.listActive(countryCode));
    }
}
