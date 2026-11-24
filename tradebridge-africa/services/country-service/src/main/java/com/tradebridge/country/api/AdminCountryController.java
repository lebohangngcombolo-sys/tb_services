package com.tradebridge.country.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.country.api.dto.request.AddCurrencyRequest;
import com.tradebridge.country.api.dto.request.AddLanguageRequest;
import com.tradebridge.country.api.dto.request.CountryStatusRequest;
import com.tradebridge.country.api.dto.request.CreateCountryRequest;
import com.tradebridge.country.api.dto.request.KycRequirementRequest;
import com.tradebridge.country.api.dto.request.OrganizationRequirementRequest;
import com.tradebridge.country.api.dto.request.UpdateCountryRequest;
import com.tradebridge.country.api.dto.response.CountryDetailResponse;
import com.tradebridge.country.api.dto.response.CurrencyResponse;
import com.tradebridge.country.api.dto.response.KycRequirementResponse;
import com.tradebridge.country.api.dto.response.LanguageResponse;
import com.tradebridge.country.api.dto.response.OrganizationRequirementResponse;
import com.tradebridge.country.service.CountryService;
import com.tradebridge.country.service.CurrencyService;
import com.tradebridge.country.service.KycRequirementService;
import com.tradebridge.country.service.LanguageService;
import com.tradebridge.country.service.OrganizationRequirementService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Country administration endpoints (spec §4.1B, §4.1C, §4.1D).
 * All require SUPER_ADMIN or an ADMIN with COUNTRY_ADMIN.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Country Admin", description = "Administrative country management")
public class AdminCountryController {

    private final CountryService countryService;
    private final CurrencyService currencyService;
    private final LanguageService languageService;
    private final OrganizationRequirementService organizationRequirementService;
    private final KycRequirementService kycRequirementService;

    public AdminCountryController(CountryService countryService,
                                  CurrencyService currencyService,
                                  LanguageService languageService,
                                  OrganizationRequirementService organizationRequirementService,
                                  KycRequirementService kycRequirementService) {
        this.countryService = countryService;
        this.currencyService = currencyService;
        this.languageService = languageService;
        this.organizationRequirementService = organizationRequirementService;
        this.kycRequirementService = kycRequirementService;
    }

    // ------------------------------------------------------------------ countries

    @PostMapping("/countries")
    @Operation(summary = "Create a country")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<CountryDetailResponse> createCountry(@Valid @RequestBody CreateCountryRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(countryService.create(request, actor), "Country created");
    }

    @PutMapping("/countries/{countryCode}")
    @Operation(summary = "Update a country")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<CountryDetailResponse> updateCountry(@PathVariable String countryCode,
                                                            @Valid @RequestBody UpdateCountryRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(countryService.update(countryCode, request, actor), "Country updated");
    }

    @PatchMapping("/countries/{countryCode}/status")
    @Operation(summary = "Enable or disable a country")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<CountryDetailResponse> changeStatus(@PathVariable String countryCode,
                                                           @Valid @RequestBody CountryStatusRequest request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(countryService.changeStatus(countryCode, request, actor),
                "Country status updated");
    }

    // ------------------------------------------------------------------ currencies

    @PostMapping("/countries/{countryCode}/currencies")
    @Operation(summary = "Add a supported currency")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<CurrencyResponse> addCurrency(@PathVariable String countryCode,
                                                     @Valid @RequestBody AddCurrencyRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(currencyService.addToCountry(countryCode, request, actor),
                "Currency added");
    }

    @DeleteMapping("/countries/{countryCode}/currencies/{currencyCode}")
    @Operation(summary = "Remove a supported currency")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<Void> removeCurrency(@PathVariable String countryCode,
                                            @PathVariable String currencyCode,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        currencyService.removeFromCountry(countryCode, currencyCode, actor);
        return ResponseFactory.ok("Currency removed");
    }

    // ------------------------------------------------------------------ languages

    @PostMapping("/countries/{countryCode}/languages")
    @Operation(summary = "Add a supported language")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<LanguageResponse> addLanguage(@PathVariable String countryCode,
                                                     @Valid @RequestBody AddLanguageRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(languageService.addToCountry(countryCode, request, actor),
                "Language added");
    }

    @DeleteMapping("/countries/{countryCode}/languages/{languageCode}")
    @Operation(summary = "Remove a supported language")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<Void> removeLanguage(@PathVariable String countryCode,
                                            @PathVariable String languageCode,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        languageService.removeFromCountry(countryCode, languageCode, actor);
        return ResponseFactory.ok("Language removed");
    }

    // ------------------------------------------------------------------ organization requirements

    @PostMapping("/countries/{countryCode}/organization-requirements")
    @Operation(summary = "Create an organization requirement")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<OrganizationRequirementResponse> createOrganizationRequirement(
            @PathVariable String countryCode,
            @Valid @RequestBody OrganizationRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                organizationRequirementService.create(countryCode, request, actor),
                "Organization requirement created");
    }

    @PutMapping("/organization-requirements/{requirementId}")
    @Operation(summary = "Update an organization requirement")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<OrganizationRequirementResponse> updateOrganizationRequirement(
            @PathVariable UUID requirementId,
            @Valid @RequestBody OrganizationRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                organizationRequirementService.update(requirementId, request, actor),
                "Organization requirement updated");
    }

    @PatchMapping("/organization-requirements/{requirementId}/status")
    @Operation(summary = "Enable or disable an organization requirement")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<OrganizationRequirementResponse> changeOrganizationRequirementStatus(
            @PathVariable UUID requirementId,
            @RequestBody java.util.Map<String, Boolean> body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ResponseFactory.ok(
                organizationRequirementService.changeStatus(requirementId, enabled, actor),
                "Status updated");
    }

    // ------------------------------------------------------------------ KYC requirements

    @PostMapping("/countries/{countryCode}/kyc-requirements")
    @Operation(summary = "Create a KYC requirement")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<KycRequirementResponse> createKycRequirement(
            @PathVariable String countryCode,
            @Valid @RequestBody KycRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycRequirementService.create(countryCode, request, actor),
                "KYC requirement created");
    }

    @PutMapping("/kyc-requirements/{requirementId}")
    @Operation(summary = "Update a KYC requirement")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<KycRequirementResponse> updateKycRequirement(
            @PathVariable UUID requirementId,
            @Valid @RequestBody KycRequirementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(
                kycRequirementService.update(requirementId, request, actor),
                "KYC requirement updated");
    }

    @PatchMapping("/kyc-requirements/{requirementId}/status")
    @Operation(summary = "Enable or disable a KYC requirement")
    @RequirePermission(PermissionCode.COUNTRY_ADMIN)
    public ApiResponse<KycRequirementResponse> changeKycRequirementStatus(
            @PathVariable UUID requirementId,
            @RequestBody java.util.Map<String, Boolean> body,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ResponseFactory.ok(
                kycRequirementService.changeStatus(requirementId, enabled, actor),
                "Status updated");
    }
}
