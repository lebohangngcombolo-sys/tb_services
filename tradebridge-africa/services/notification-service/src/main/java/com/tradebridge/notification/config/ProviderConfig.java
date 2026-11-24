package com.tradebridge.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires provider configuration beans and a shared {@link RestClient.Builder}.
 *
 * <p>Property classes are enabled here (in addition to
 * {@code @ConfigurationPropertiesScan} on the app class) so that provider
 * tests can instantiate the config in isolation.
 */
@Configuration
@EnableConfigurationProperties({
        com.tradebridge.notification.provider.email.SmtpProperties.class,
        com.tradebridge.notification.provider.sms.SmsProviderProperties.class
})
public class ProviderConfig {

    /**
     * Shared {@link RestClient.Builder} used by the SMS providers. Adding
     * interceptors (retry, correlation header propagation) is a one-line
     * change here that affects every outbound call.
     */
    @org.springframework.context.annotation.Bean
    public RestClient.Builder providerRestClientBuilder() {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    String correlationId =
                            com.tradebridge.common.context.CorrelationContext.current();
                    if (correlationId != null) {
                        request.getHeaders().add(
                                com.tradebridge.observability.correlation.CorrelationIdPropagation.HEADER,
                                correlationId);
                    }
                    return execution.execute(request, body);
                });
    }
}