package com.tradebridge.identity.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.util.JsonUtils;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.domain.enums.UserStatus;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIT {

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

    private static final String EMAIL = "login.user@example.com";
    private static final String PHONE = "+27123456789";
    private static final String PASSWORD = "Aa123456789!";

    @BeforeEach
    void setup() {
        credentialRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User(UUID.randomUUID(), "Login", "User", EMAIL, PHONE, "ZA", "EN");
        user.markEmailVerified();
        user.activate();
        userRepository.save(user);
        credentialRepository.save(new UserCredential(user.getId(), passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void loginSuccessByEmail() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.mfaRequired").value(false))
                .andReturn();

        JsonNode node = JsonUtils.readTree(result.getResponse().getContentAsString());
        String accessToken = node.path("data").path("accessToken").asText();
        assertThat(accessToken).contains(".");
    }

    @Test
    void loginSuccessByPhone() throws Exception {
        mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", PHONE, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", EMAIL, "password", "WrongPassword!1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginUnknownUser() throws Exception {
        mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", "no.such@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSuspendedUserRejected() throws Exception {
        userRepository.findByEmail(EMAIL).ifPresent(u -> {
            u.suspend();
            userRepository.save(u);
        });
        mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", EMAIL, "password", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    void refreshRotatesTokens() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String firstRefresh = JsonUtils.readTree(login.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        MvcResult refresh = mockMvc.perform(post("/api/v1/identity/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isOk())
                .andReturn();

        String newRefresh = JsonUtils.readTree(refresh.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(firstRefresh);
    }

    @Test
    void refreshReuseRevokesAllSessions() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("identifier", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String firstRefresh = JsonUtils.readTree(login.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // First use rotates successfully
        mockMvc.perform(post("/api/v1/identity/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isOk());

        // Second use of the same token → reuse detected
        mockMvc.perform(post("/api/v1/identity/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isUnauthorized());
    }
}
