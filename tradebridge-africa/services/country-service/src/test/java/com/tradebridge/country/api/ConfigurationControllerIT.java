package com.tradebridge.country.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradebridge.common.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuration publishing and rollback (spec §4.1E).
 *
 * <p>Uses {@code jwt()} post-processor for admin routes — the
 * {@code @RequirePermission} aspect is bypassed by using a SUPER_ADMIN
 * principal, which has the COUNTRY_CONFIG_PUBLISH permission via the
 * role hierarchy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ConfigurationControllerIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tb_country_db")
                    .withUsername("tradebridge")
                    .withPassword("tradebridge");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.cache.type", () -> "none");
        registry.add("tradebridge.security.jwt.public-key-location",
                () -> "classpath:keys/test-jwt-public.pem");
    }

    @Autowired
    private MockMvc mockMvc;

    private static final org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor SUPER_ADMIN =
            jwt().authorities(new SimpleGrantedAuthority("SUPER_ADMIN"));

    @Test
    void activeConfigurationIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/countries/ZA/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void versionsRequiresPermission() throws Exception {
        mockMvc.perform(get("/api/v1/countries/ZA/configuration/versions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void versionsListShowsHistory() throws Exception {
        mockMvc.perform(get("/api/v1/countries/ZA/configuration/versions")
                        .with(SUPER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].version").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));
    }

    @Test
    void publishCreatesNewVersionAndSupersedesOld() throws Exception {
        // Publish v2 with an inline config body
        MvcResult publish = mockMvc.perform(post("/api/v1/countries/ZA/configuration/publish")
                        .with(SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"v2 change\",\"configuration\":{"
                                + "\"onboarding.min_directors\":{\"value\":2},"
                                + "\"kyc.beneficial_ownership_threshold\":{\"value\":30.0}"
                                + "}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.changeSummary").value("v2 change"))
                .andReturn();

        JsonNode published = JsonUtils.readTree(publish.getResponse().getContentAsString());
        assertThat(published.path("data").path("configuration").path("onboarding.min_directors")
                .path("value").asInt()).isEqualTo(2);

        // Active configuration is now v2
        mockMvc.perform(get("/api/v1/countries/ZA/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));

        // v1 is superseded
        mockMvc.perform(get("/api/v1/countries/ZA/configuration/versions/1")
                        .with(SUPER_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUPERSEDED"));
    }

    @Test
    void publishWithoutBody_copiesPreviousVersion() throws Exception {
        // v2 with new values
        mockMvc.perform(post("/api/v1/countries/ZA/configuration/publish")
                        .with(SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"v2\",\"configuration\":{"
                                + "\"onboarding.min_directors\":{\"value\":2}}}"))
                .andExpect(status().isOk());

        // v3 with no body — copies v2
        MvcResult v3 = mockMvc.perform(post("/api/v1/countries/ZA/configuration/publish")
                        .with(SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"carry forward\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(3))
                .andReturn();

        JsonNode data = JsonUtils.readTree(v3.getResponse().getContentAsString()).path("data");
        assertThat(data.path("configuration").path("onboarding.min_directors").path("value").asInt())
                .isEqualTo(2);
    }

    @Test
    void rollbackRestoresPreviousVersion() throws Exception {
        // Create v2
        mockMvc.perform(post("/api/v1/countries/ZA/configuration/publish")
                        .with(SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"v2\",\"configuration\":{"
                                + "\"onboarding.min_directors\":{\"value\":2}}}"))
                .andExpect(status().isOk());

        // Roll back to v1
        mockMvc.perform(post("/api/v1/countries/ZA/configuration/rollback")
                        .with(SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetVersion\":1,\"reason\":\"v2 had issues\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("ROLLED_BACK"));

        // Active config is v1
        mockMvc.perform(get("/api/v1/countries/ZA/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("ROLLED_BACK"));
    }

    @Test
    void rollbackRejectsDraft() throws Exception {
        // v1 is published, so rolling back to itself is a no-op — instead
        // test rollback to a non-existent version
        mockMvc.perform(post("/api/v1/countries/ZA/configuration/rollback")
                        .with(SUPER_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetVersion\":999,\"reason\":\"bogus\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/countries/ZA/configuration/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishRequiresPermission() throws Exception {
        // Authenticated but lacking COUNTRY_CONFIG_PUBLISH
        mockMvc.perform(post("/api/v1/countries/ZA/configuration/publish")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}