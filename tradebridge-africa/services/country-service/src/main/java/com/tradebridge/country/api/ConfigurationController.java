package com.tradebridge.country.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.country.api.dto.request.PublishConfigurationRequest;
import com.tradebridge.country.api.dto.request.RollbackConfigurationRequest;
import com.tradebridge.country.api.dto.response.ConfigurationResponse;
import com.tradebridge.country.api.dto.response.ConfigurationVersionResponse;
import com.tradebridge.country.service.ConfigurationService;
import com.tradebridge.country.service.ConfigurationVersionService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Configuration versioning (spec §4.1E).
 *
 * <p>Public: active configuration. Admin: versions list, publish, rollback.
 * Published versions are immutable — changes create a new version
 * (spec §4.4).
 */
@RestController
@RequestMapping("/api/v1/countries/{countryCode}/configuration")
@Tag(name = "Configuration", description = "Country configuration versioning")
public class ConfigurationController {

    private final ConfigurationService configurationService;
    private final ConfigurationVersionService versionService;

    public ConfigurationController(ConfigurationService configurationService,
                                   ConfigurationVersionService versionService) {
        this.configurationService = configurationService;
        this.versionService = versionService;
    }

    @GetMapping
    @Operation(summary = "Get the currently active configuration")
    public ApiResponse<ConfigurationResponse> active(@PathVariable String countryCode) {
        return ResponseFactory.ok(configurationService.getActive(countryCode));
    }

    @GetMapping("/versions")
    @Operation(summary = "List configuration versions")
    @RequirePermission(PermissionCode.COUNTRY_CONFIG_PUBLISH)
    public ApiResponse<List<ConfigurationVersionResponse>> versions(@PathVariable String countryCode) {
        return ResponseFactory.ok(versionService.listVersions(countryCode));
    }

    @GetMapping("/versions/{version}")
    @Operation(summary = "Retrieve a specific configuration version")
    @RequirePermission(PermissionCode.COUNTRY_CONFIG_PUBLISH)
    public ApiResponse<ConfigurationResponse> version(@PathVariable String countryCode,
                                                      @PathVariable int version) {
        return ResponseFactory.ok(configurationService.getVersion(countryCode, version));
    }

    @PostMapping("/publish")
    @Operation(summary = "Publish a new configuration version")
    @RequirePermission(PermissionCode.COUNTRY_CONFIG_PUBLISH)
    public ApiResponse<ConfigurationResponse> publish(@PathVariable String countryCode,
                                                      @Valid @RequestBody PublishConfigurationRequest request,
                                                      @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(configurationService.publish(countryCode, request, actorId),
                "Configuration published");
    }

    @PostMapping("/rollback")
    @Operation(summary = "Roll back to a previous configuration version")
    @RequirePermission(PermissionCode.COUNTRY_CONFIG_ROLLBACK)
    public ApiResponse<ConfigurationResponse> rollback(@PathVariable String countryCode,
                                                       @Valid @RequestBody RollbackConfigurationRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(configurationService.rollback(countryCode, request, actorId),
                "Configuration rolled back");
    }
}
