package com.tradebridge.country.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.country.api.dto.response.LanguageResponse;
import com.tradebridge.country.service.LanguageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public language endpoints (spec §4.1A).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Languages", description = "Public language information")
public class LanguageController {

    private final LanguageService languageService;

    public LanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping("/languages")
    @Operation(summary = "List all supported languages")
    public ApiResponse<List<LanguageResponse>> list() {
        return ResponseFactory.ok(languageService.listEnabled());
    }

    @GetMapping("/countries/{countryCode}/languages")
    @Operation(summary = "Get languages supported by a country")
    public ApiResponse<List<LanguageResponse>> listForCountry(@PathVariable String countryCode) {
        return ResponseFactory.ok(languageService.listForCountry(countryCode));
    }
}
