package com.tradebridge.customer.config;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.observability.correlation.CorrelationIdPropagation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Outbound HTTP client configuration (spec §6.3).
 *
 * <p>Named {@code FeignClientConfig} per the module structure for
 * historical reasons — the actual client is Spring's {@link RestClient}
 * with the JDK HTTP client. If the team later migrates to OpenFeign,
 * this is the config class that changes.
 *
 * <p>Every outbound call:
 * <ul>
 *   <li>Propagates {@code X-Correlation-Id} from the current context.</li>
 *   <li>Uses bounded connect and read timeouts.</li>
 *   <li>Never carries plaintext OTPs or tokens (callers pass only IDs).</li>
 * </ul>
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RestClient.Builder customerRestClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = CorrelationContext.current();
                    if (correlationId != null) {
                        request.getHeaders().add(
                                CorrelationIdPropagation.HEADER, correlationId);
                    }
                    return execution.execute(request, body);
                });
    }
}