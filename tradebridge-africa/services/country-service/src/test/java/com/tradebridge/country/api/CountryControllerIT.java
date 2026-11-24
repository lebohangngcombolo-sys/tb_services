package com.tradebridge.country.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradebridge.common.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public read endpoints (spec §4.1A). No authentication required.
 *
 * <p>Relies on Flyway V2 seed data (ZA + PT) being present.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CountryControllerIT {

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
        // Disable Redis cache — tests exercise the underlying reads
        registry.add("spring.cache.type", () -> "none");
        // Stub JWKS — public endpoints do not touch it, but the decoder bean must exist
        registry.add("tradebridge.security.jwt.public-key-location",
                () -> "classpath:keys/test-jwt-public.pem");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsEnabledCountries() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode node = JsonUtils.readTree(result.getResponse().getContentAsString());
        JsonNode data = node.path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(2);

        boolean hasZA = false;
        boolean hasPT = false;
        for (JsonNode country : data) {
            String iso2 = country.path("iso2Code").asText();
            if ("ZA".equals(iso2)) hasZA = true;
            if ("PT".equals(iso2)) hasPT = true;
        }
        assertThat(hasZA).isTrue();
        assertThat(hasPT).isTrue();
    }

    @Test
    void returnsCountryDetail() throws Exception {
        mockMvc.perform(get("/api/v1/countries/ZA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.iso2Code").value("ZA"))
                .andExpect(jsonPath("$.data.iso3Code").value("ZAF"))
                .andExpect(jsonPath("$.data.name").value("South Africa"))
                .andExpect(jsonPath("$.data.defaultCurrencyCode").value("ZAR"))
                .andExpect(jsonPath("$.data.defaultLanguageCode").value("EN"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.currencies").isArray())
                .andExpect(jsonPath("$.data.languages").isArray());
    }

    @Test
    void countryLookupIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/v1/countries/za"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.iso2Code").value("ZA"));
    }

    @Test
    void returns404ForUnknownCountry() throws Exception {
        mockMvc.perform(get("/api/v1/countries/XX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listsCurrencies() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listsCurrenciesForCountry() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/countries/ZA/currencies"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = JsonUtils.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);

        boolean foundZarDefault = false;
        for (JsonNode currency : data) {
            if ("ZAR".equals(currency.path("code").asText())
                    && currency.path("isDefault").asBoolean()) {
                foundZarDefault = true;
            }
        }
        assertThat(foundZarDefault).isTrue();
    }

    @Test
    void listsLanguages() throws Exception {
        mockMvc.perform(get("/api/v1/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listsLanguagesForCountry() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/countries/PT/languages"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = JsonUtils.readTree(result.getResponse().getContentAsString()).path("data");
        boolean hasPt = false;
        for (JsonNode lang : data) {
            if ("pt".equals(lang.path("code").asText())) {
                hasPt = true;
                assertThat(lang.path("isDefault").asBoolean()).isTrue();
            }
        }
        assertThat(hasPt).isTrue();
    }

    @Test
    void listsRequirements() throws Exception {
        mockMvc.perform(get("/api/v1/countries/ZA/requirements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countryCode").value("ZA"))
                .andExpect(jsonPath("$.data.organizationRequirements").isArray())
                .andExpect(jsonPath("$.data.kycRequirements").isArray());
    }

    @Test
    void listsKycRequirements() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/countries/ZA/kyc-requirements"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = JsonUtils.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void listsOrganizationRequirements() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/countries/ZA/organization-requirements"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = JsonUtils.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void returnsActiveConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/countries/ZA/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countryCode").value("ZA"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.configuration").exists());
    }
}