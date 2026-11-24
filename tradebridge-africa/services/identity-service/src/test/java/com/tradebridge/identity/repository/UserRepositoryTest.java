package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tb_identity_db")
                    .withUsername("tradebridge")
                    .withPassword("tradebridge");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void findByEmail_returnsUser() {
        User user = new User(UUID.randomUUID(), "Test", "User", "u@example.com", "+27123456789", "ZA", "EN");
        userRepository.save(user);

        assertThat(userRepository.findByEmail("u@example.com")).isPresent();
        assertThat(userRepository.findByEmail("nope@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_detectsDuplicates() {
        userRepository.save(new User(UUID.randomUUID(), "A", "B", "dup@example.com", "+27123456780", "ZA", "EN"));
        assertThat(userRepository.existsByEmail("dup@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    void findByPhoneNumber_works() {
        userRepository.save(new User(UUID.randomUUID(), "A", "B", "p@example.com", "+27123456781", "ZA", "EN"));
        assertThat(userRepository.findByPhoneNumber("+27123456781")).isPresent();
    }

    @Test
    void search_findsByEmailAndName() {
        userRepository.save(new User(UUID.randomUUID(), "Alice", "Smith", "alice@example.com", "+27123456782", "ZA", "EN"));
        userRepository.save(new User(UUID.randomUUID(), "Bob", "Jones", "bob@example.com", "+27123456783", "ZA", "EN"));

        Page<User> result = userRepository.search("alice", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@example.com");

        Page<User> byName = userRepository.search("smith", PageRequest.of(0, 10));
        assertThat(byName.getContent()).hasSize(1);
    }

    @Test
    void findByStatus_filters() {
        User active = new User(UUID.randomUUID(), "A", "B", "a@example.com", "+27123456784", "ZA", "EN");
        active.activate();
        userRepository.save(active);

        User pending = new User(UUID.randomUUID(), "C", "D", "c@example.com", "+27123456785", "ZA", "EN");
        userRepository.save(pending);

        Page<User> page = userRepository.findByStatus(UserStatus.ACTIVE,
                PageRequest.of(0, 10, Sort.by("createdAt").descending()));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("a@example.com");
    }
}
