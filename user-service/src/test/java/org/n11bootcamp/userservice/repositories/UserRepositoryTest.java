package org.n11bootcamp.userservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.userservice.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("user_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    private static final String EMAIL = "user@example.com";
    private static final UUID KEYCLOAK_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    private User createSampleUser(String email, UUID keycloakId) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .keycloakId(keycloakId)
                .firstName("Murat")
                .lastName("Ertik")
                .build();
    }

    @Test
    @DisplayName("findByEmail: should return user when exists")
    void findByEmail_returnsUser_whenExists() {
        userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        Optional<User> found = userRepository.findByEmail(EMAIL);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("findByEmail: should return empty when email does not exist")
    void findByEmail_returnsEmpty_whenNotFound() {
        userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        Optional<User> found = userRepository.findByEmail("missing@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByEmail: should be case sensitive")
    void findByEmail_isCaseSensitive() {
        userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        Optional<User> found = userRepository.findByEmail(EMAIL.toUpperCase());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByKeycloakId: should return user when exists")
    void findByKeycloakId_returnsUser_whenExists() {
        userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        Optional<User> found = userRepository.findByKeycloakId(KEYCLOAK_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getKeycloakId()).isEqualTo(KEYCLOAK_ID);
    }

    @Test
    @DisplayName("findByKeycloakId: should return empty when id does not exist")
    void findByKeycloakId_returnsEmpty_whenNotFound() {
        userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        Optional<User> found = userRepository.findByKeycloakId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail: should return true when exists")
    void existsByEmail_returnsTrue_whenExists() {
        userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        boolean exists = userRepository.existsByEmail(EMAIL);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail: should return false when email does not exist")
    void existsByEmail_returnsFalse_whenNotFound() {
        userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        boolean exists = userRepository.existsByEmail("missing@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByEmail: should return false when repository is empty")
    void existsByEmail_returnsFalse_whenRepositoryEmpty() {
        boolean exists = userRepository.existsByEmail(EMAIL);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("save: should persist user and assign id")
    void save_persistsUser_andAssignsId() {
        User saved = userRepository.save(createSampleUser(EMAIL, KEYCLOAK_ID));

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("findByEmail: should distinguish between multiple users")
    void findByEmail_distinguishesBetweenUsers() {
        userRepository.save(createSampleUser("first@example.com", UUID.randomUUID()));
        userRepository.save(createSampleUser("second@example.com", UUID.randomUUID()));

        Optional<User> found = userRepository.findByEmail("second@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("second@example.com");
    }
}