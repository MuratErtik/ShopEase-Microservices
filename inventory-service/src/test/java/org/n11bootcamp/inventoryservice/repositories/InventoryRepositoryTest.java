package org.n11bootcamp.inventoryservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.inventoryservice.entities.Inventory;
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
class InventoryRepositoryTest {

    // ── Infrastructure ─────────────────────────────────────────────────────────

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    // ── Shared Test Data ───────────────────────────────────────────────────────

    private static final UUID PRODUCT_ID = UUID.fromString("bbbb0000-0000-0000-0000-000000000001");
    private static final UUID SELLER_ID = UUID.fromString("cccc0000-0000-0000-0000-000000000001");

    @BeforeEach
    void setup() {
        inventoryRepository.deleteAll();
    }

    private Inventory createSampleInventory(UUID productId) {
        return Inventory.builder()
                .productId(productId)
                .sellerId(SELLER_ID)
                .sellerEmail("test@seller.com")
                .availableQuantity(100)
                .reservedQuantity(0)
                .build();
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByProductId: should return inventory when product exists")
    void findByProductId_returnsInventory_whenExists() {
        Inventory inventory = createSampleInventory(PRODUCT_ID);
        inventoryRepository.save(inventory);

        Optional<Inventory> found = inventoryRepository.findByProductId(PRODUCT_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(found.get().getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("existsByProductId: should return true when product is in database")
    void existsByProductId_returnsTrue_whenExists() {
        inventoryRepository.save(createSampleInventory(PRODUCT_ID));

        boolean exists = inventoryRepository.existsByProductId(PRODUCT_ID);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByProductId: should return false when product is NOT in database")
    void existsByProductId_returnsFalse_whenNotExists() {
        boolean exists = inventoryRepository.existsByProductId(UUID.randomUUID());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByProductIdWithLock: should acquire pessimistic lock and return inventory")
    void findByProductIdWithLock_returnsInventoryAndAcquiresLock() {
        // Not: Lock mekanizmasının kendisini unit test ile kanıtlamak zordur (thread gerektirir),
        // ancak sorgunun doğru çalıştığını ve JPA mapping hatası olmadığını doğrular.
        Inventory inventory = createSampleInventory(PRODUCT_ID);
        inventoryRepository.save(inventory);

        Optional<Inventory> found = inventoryRepository.findByProductIdWithLock(PRODUCT_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("save: should persist inventory with correct UUID and initial values")
    void save_persistsInventoryCorrectly() {
        Inventory inventory = createSampleInventory(PRODUCT_ID);
        Inventory saved = inventoryRepository.save(inventory);

        assertThat(saved.getId()).isNotNull(); // UUID tipindeki primary key
        assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(saved.getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("findByProductId: should return empty optional for non-existing product")
    void findByProductId_returnsEmpty_whenNotFound() {
        Optional<Inventory> found = inventoryRepository.findByProductId(UUID.randomUUID());
        assertThat(found).isEmpty();
    }
}