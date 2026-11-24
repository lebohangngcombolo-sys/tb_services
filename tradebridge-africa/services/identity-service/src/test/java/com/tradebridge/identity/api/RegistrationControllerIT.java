package com.tradebridge.identity.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.util.JsonUtils;
import com.tradebridge.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flow for registration, using a real Postgres and Kafka.
 *
 * <p>Country validation is bypassed by pointing the country client at a
 * local mock server — see {@link TestConfig}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class RegistrationControllerIT {

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
        // Disable real Kafka consumers during this test (Kafka container still runs)
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        // Country service always says "enabled"
        registry.add("tradebridge.identity.country-service.base-url", () -> "http://localhost:9999");
        registry.add("tradebridge.identity.country-service.allow-any-country", () -> "true");
        // Skip signing key requirement — this test does not sign tokens
        registry.add("tradebridge.security.jwt.public-key-location",
                () -> "classpath:keys/test-jwt-public.pem");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void registersNewUser() throws Exception {
        Map<String, Object> body = Map.of(
                "firstName", "Test",
                "lastName", "User",
                "email", "test.registration@example.com",
                "phoneNumber", "+27123456789",
                "password", "Aa123456789!",
                "countryCode", "ZA",
                "preferredLanguage", "EN",
                "termsAccepted", "true");

        MvcResult result = mockMvc.perform(post("/api/v1/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test.registration@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andReturn();

        JsonNode node = JsonUtils.readTree(result.getResponse().getContentAsString());
        String userId = node.path("data").path("userId").asText();
        assertThat(userId).isNotBlank();
        assertThat(userRepository.findByEmail("test.registration@example.com")).isPresent();
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        Map<String, Object> body = Map.of(
                "firstName", "Test",
                "lastName", "User",
                "email", "dup@example.com",
                "phoneNumber", "+27123456789",
                "password", "Aa123456789!",
                "countryCode", "ZA",
                "preferredLanguage", "EN",
                "termsAccepted", "true");

        mockMvc.perform(post("/api/v1/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        Map<String, Object> dup = Map.of(
                "firstName", "Other",
                "lastName", "User",
                "email", "dup@example.com",
                "phoneNumber", "+27123456780",
                "password", "Aa123456789!",
                "countryCode", "ZA",
                "preferredLanguage", "EN",
                "termsAccepted", "true");

        mockMvc.perform(post("/api/v1/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void rejectsInvalidEmailFormat() throws Exception {
        Map<String, Object> body = Map.of(
                "firstName", "Test",
                "lastName", "User",
                "email", "not-an-email",
                "phoneNumber", "+27123456789",
                "password", "Aa123456789!",
                "countryCode", "ZA",
                "preferredLanguage", "EN",
                "termsAccepted", "true");

        mockMvc.perform(post("/api/v1/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsWeakPassword() throws Exception {
        Map<String, Object> body = Map.of(
                "firstName", "Test",
                "lastName", "User",
                "email", "weak@example.com",
                "phoneNumber", "+27123456789",
                "password", "short",
                "countryCode", "ZA",
                "preferredLanguage", "EN",
                "termsAccepted", "true");

        mockMvc.perform(post("/api/v1/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
