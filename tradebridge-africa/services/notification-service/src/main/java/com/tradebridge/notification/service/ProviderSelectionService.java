package com.tradebridge.notification.service;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.notification.api.dto.response.ProviderStatusResponse;
import com.tradebridge.notification.config.NotificationProperties;
import com.tradebridge.notification.provider.NotificationProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chooses the primary provider per channel (spec §5.5). Keeps the
 * provider adapters replaceable — switching from Africa's Talking to
 * Twilio is a config change, not a code change.
 */
@Service
public class ProviderSelectionService {

    private final Map<String, NotificationProvider> byName;
    private final List<NotificationProvider> allProviders;
    private final NotificationProperties properties;

    public ProviderSelectionService(List<NotificationProvider> providers,
                                     NotificationProperties properties) {
        this.allProviders = List.copyOf(providers);
        this.byName = providers.stream()
                .collect(Collectors.toMap(NotificationProvider::name, p -> p));
        this.properties = properties;
    }

    /**
     * Returns the primary provider for a channel. Fails loudly if none is
     * configured — a missing provider is a deployment error, not a runtime
     * decision.
     */
    public NotificationProvider primaryFor(Channel channel) {
        String wantedName = switch (channel) {
            case EMAIL -> "smtp";
            case SMS -> properties.getProviders().getSms().getPrimary();
            case IN_APP -> "database-in-app";
        };
        NotificationProvider provider = byName.get(wantedName);
        if (provider == null) {
            throw new IllegalStateException("No provider registered for '" + wantedName
                    + "' (channel " + channel + ")");
        }
        return provider;
    }

    /**
     * Fallback provider for a channel, if any. SMS can have both Africa's
     * Talking and Twilio configured; EMAIL only has SMTP.
     */
    public List<NotificationProvider> candidatesFor(Channel channel) {
        return allProviders.stream()
                .filter(p -> p.channel() == channel && p.isEnabled())
                .toList();
    }

    public List<ProviderStatusResponse> providerStatuses() {
        return allProviders.stream()
                .map(p -> new ProviderStatusResponse(
                        p.name(),
                        p.channel().name(),
                        p.isEnabled(),
                        p.healthCheck(),
                        p.isEnabled() ? "configured" : "disabled"))
                .toList();
    }
}