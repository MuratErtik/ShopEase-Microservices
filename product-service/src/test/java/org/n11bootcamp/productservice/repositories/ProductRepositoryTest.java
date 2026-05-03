package org.n11bootcamp.productservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.productservice.entities.Product;
import org.n11bootcamp.productservice.enums.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("product_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
    }

    private Product createSampleProduct(String name, String brand, String color) {
        return Product.builder()
                .name(name)
                .brand(brand)
                .color(color)
                .description("Test Description")
                .price(new BigDecimal("1500.00"))
                .category(Category.ELECTRONICS)
                .sellerId(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("existsByName: should return true when exact name exists")
    void existsByName_returnsTrue_whenExactNameExists() {
        productRepository.save(createSampleProduct("iPhone 15", "Apple", "Blue"));

        boolean exists = productRepository.existsByName("iPhone 15");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase: should return true regardless of case")
    void existsByNameIgnoreCase_returnsTrue_whenCaseDiffers() {
        productRepository.save(createSampleProduct("iPhone 15", "Apple", "Blue"));

        boolean exists = productRepository.existsByNameIgnoreCase("iphone 15");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByName: should return false when name does not exist")
    void existsByName_returnsFalse_whenNotExists() {
        productRepository.save(createSampleProduct("iPhone 15", "Apple", "Blue"));

        boolean exists = productRepository.existsByName("Samsung S23");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndBrandAndColorIgnoreCase: should return false if one criteria mismatch")
    void existsByNameAndBrandAndColorIgnoreCase_returnsFalse_whenColorMismatch() {
        productRepository.save(createSampleProduct("AirPods", "Apple", "White"));

        boolean exists = productRepository.existsByNameAndBrandAndColorIgnoreCase("AirPods", "Apple", "Black");

        assertThat(exists).isFalse();
    }
}