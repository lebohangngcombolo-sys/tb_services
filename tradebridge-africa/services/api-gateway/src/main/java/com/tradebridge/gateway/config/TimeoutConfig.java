package com.tradebridge.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Enforces connection and response timeouts on outbound calls (spec §2.5
 * "Apply connection and response timeouts").
 *
 * <p>The {@code GlobalFilter} layer (see {@code DownstreamUnavailableFilter})
 * uses this {@link WebClient} builder for fallback handling. Gateway routes
 * themselves derive their timeouts from the route metadata or from these
 * defaults.
 */
@Configuration
public class TimeoutConfig {

    @Bean
    public HttpClient gatewayHttpClient(GatewayProperties properties) {
        GatewayProperties.Timeout t = properties.getTimeout();
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) t.getConnect().toMillis())
                .responseTimeout(t.getResponse())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(t.getResponse().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(t.getResponse().toMillis(), TimeUnit.MILLISECONDS)));
    }

    @Bean
    public WebClient.Builder gatewayWebClientBuilder(HttpClient gatewayHttpClient) {
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(gatewayHttpClient));
    }

    /**
     * Response timeout specifically for gateway route forwarding. Spring
     * Cloud Gateway picks up a bean named {@code httpClient} if present —
     * we register it explicitly so route-level filters can use it too.
     */
    @Bean
    public HttpClient httpClient(HttpClient gatewayHttpClient) {
        return gatewayHttpClient;
    }
}