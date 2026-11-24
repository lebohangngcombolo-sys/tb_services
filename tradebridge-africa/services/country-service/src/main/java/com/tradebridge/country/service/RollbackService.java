package com.tradebridge.country.service;

import com.tradebridge.country.api.dto.request.RollbackConfigurationRequest;
import com.tradebridge.country.api.dto.response.ConfigurationResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Facade over {@link ConfigurationService#rollback(String, RollbackConfigurationRequest, UUID)}
 * kept as a separate file so future rollback-specific concerns (audit,
 * approval workflow, notifications) have a natural home.
 */
@Service
public class RollbackService {

    private final ConfigurationService configurationService;

    public RollbackService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public ConfigurationResponse rollback(String countryCode,
                                           RollbackConfigurationRequest request,
                                           UUID actorId) {
        return configurationService.rollback(countryCode, request, actorId);
    }
}