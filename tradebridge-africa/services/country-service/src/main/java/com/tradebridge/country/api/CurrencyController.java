package com.tradebridge.country.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.country.api.dto.response.CurrencyResponse;
import com.tradebridge.country.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public currency endpoints (spec §4.1A).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Currencies", description = "Public currency information")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping("/currencies")
    @Operation(summary = "List all supported currencies")
    public ApiResponse<List<CurrencyResponse>> list() {
        return ResponseFactory.ok(currencyService.listEnabled());
    }

    @GetMapping("/countries/{countryCode}/currencies")
    @Operation(summary = "Get currencies supported by a country")
    public ApiResponse<List<CurrencyResponse>> listForCountry(@PathVariable String countryCode) {
        return ResponseFactory.ok(currencyService.listForCountry(countryCode));
    }
}
