package com.tradebridge.identity.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.util.JsonUtils;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class MfaControllerIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tb_identity_db")
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
        registry.add("tradebridge.security.jwt.public-key-location",
                () -> "classpath:keys/test-jwt-public.pem");
        registry.add("tradebridge.security.jwt.private-key-location",
                () -> "classpath:keys/test-jwt-private.pem");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCredentialRepository credentialRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL = "mfa.user@example.com";
    private static final String PASSWORD = "Aa123456789!";

    private String accessToken;

    @BeforeEach
    void setup() throws Exception {
        credentialRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User(UUID.randomUUID(), "Mfa", "User", EMAIL, "+27123456789", "ZA", "EN");
        user.markEmailVerified();
        user.activate();
        userRepository.save(user);
        credentialRepository.save(new UserCredential(user.getId(), passwordEncoder.encode(PASSWORD)));

        // Login to get a token
        MvcResult login = mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = JsonUtils.readTree(login.getResponse().getContentAsString());
        accessToken = body.path("data").path("accessToken").asText();
    }

    @Test
    void getStatus_whenDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/identity/mfa/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void fullSetupAndVerifyFlow() throws Exception {
        MvcResult setup = mockMvc.perform(post("/api/v1/identity/mfa/setup/totp")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provisioningUri").exists())
                .andReturn();

        String provisioningUri = JsonUtils.readTree(setup.getResponse().getContentAsString())
                .path("data").path("provisioningUri").asText();
        String secret = extractSecret(provisioningUri);
        String code = new DefaultCodeGenerator().generate(secret, currentTimePeriod());

        MvcResult verify = mockMvc.perform(post("/api/v1/identity/mfa/verify-setup")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode codes = JsonUtils.readTree(verify.getResponse().getContentAsString()).path("data");
        assertThat(codes.isArray()).isTrue();
        assertThat(codes.size()).isEqualTo(10);
    }

    @Test
    void setupRequiresPassword() throws Exception {
        mockMvc.perform(post("/api/v1/identity/mfa/setup/totp")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "WrongPassword!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/identity/mfa/status"))
                .andExpect(status().isUnauthorized());
    }

    private static String extractSecret(String uri) {
        int idx = uri.indexOf("secret=");
        int end = uri.indexOf('&', idx);
        return end < 0 ? uri.substring(idx + 7) : uri.substring(idx + 7, end);
    }

    private static long currentTimePeriod() {
        return System.currentTimeMillis() / 30_000L;
    }
}
