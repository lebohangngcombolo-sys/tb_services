package com.tradebridge.country.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.country.api.dto.response.CountryDetailResponse;
import com.tradebridge.country.api.dto.response.CountryResponse;
import com.tradebridge.country.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public country information endpoints (spec §4.1A).
 * No JWT required — these are part of the public allowlist (spec §2.2).
 */
@RestController
@RequestMapping("/api/v1/countries")
@Tag(name = "Countries", description = "Public country information")
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    @Operation(summary = "List enabled countries")
    public ApiResponse<List<CountryResponse>> listEnabled() {
        return ResponseFactory.ok(countryService.listEnabled());
    }

    @GetMapping("/{countryCode}")
    @Operation(summary = "Get country details")
    public ApiResponse<CountryDetailResponse> get(@PathVariable String countryCode) {
        return ResponseFactory.ok(countryService.getDetail(countryCode));
    }
}
