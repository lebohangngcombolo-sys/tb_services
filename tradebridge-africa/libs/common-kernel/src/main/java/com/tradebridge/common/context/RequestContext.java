package com.tradebridge.common.context;


import java.util.Optional;
import java.util.UUID;

/**
 * Represents metadata associated with an HTTP request
 * or asynchronous message.
 *
 * Never store passwords, tokens or sensitive credentials here.
 */
public record RequestContext(
    String correlationId,
    UUID userId,
    String username,
    String tenantId,
    String countryCode,
    String clientId,
    String ipAddress
) {

    public RequestContext {
        correlationId = correlationId == null || correlationId.isBlank()
            ? CorrelationContext.currentOrGenerate()
            : correlationId.trim();
    }

    public Optional<UUID> userIdOptional() {
        return Optional.ofNullable(userId);
    }

    public Optional<String> usernameOptional() {
        return Optional.ofNullable(username);
    }

    public Optional<String> tenantIdOptional() {
        return Optional.ofNullable(tenantId);
    }

    public Optional<String> countryCodeOptional() {
        return Optional.ofNullable(countryCode);
    }

    public Optional<String> clientIdOptional() {
        return Optional.ofNullable(clientId);
    }

    public Optional<String> ipAddressOptional() {
        return Optional.ofNullable(ipAddress);
    }

    public static RequestContext anonymous(String correlationId) {
        return new RequestContext(
            correlationId, null, null, null, null, null, null
        );
    }
}

