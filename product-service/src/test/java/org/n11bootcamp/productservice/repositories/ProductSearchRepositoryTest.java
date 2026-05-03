package org.n11bootcamp.productservice.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.productservice.documents.ProductDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@DisplayName("ProductSearchRepository Integration Tests")
class ProductSearchRepositoryTest {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.4"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris",
                () -> "http://" + ELASTICSEARCH.getHttpHostAddress());
    }

    @Autowired
    private ProductSearchRepository repository;

    @Autowired
    private ElasticsearchOperations operations;

    @BeforeEach
    void setUp() {
        IndexOperations indexOps = operations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();
        indexOps.putMapping();

        repository.saveAll(List.of(
                product("1", "iPhone 15",       "Phone",    "Apple",   "1500.00"),
                product("2", "Galaxy S24",      "Phone",    "Samsung", "1200.00"),
                product("3", "MacBook Pro",     "Laptop",   "Apple",   "2500.00"),
                product("4", "ThinkPad X1",     "Laptop",   "Lenovo",  "1800.00"),
                product("5", "AirPods Pro",     "Audio",    "apple",   "250.00"),
                product("6", "Buds Pro",        "Audio",    "SAMSUNG", "200.00"),
                product("7", "Watch Series 9",  "Wearable", "Apple",   "500.00"),
                product("8", "Pixel 8",         "Phone",    "Google",  "999.99")
        ));

        operations.indexOps(ProductDocument.class).refresh();
    }

    @AfterEach
    void tearDown() {
        IndexOperations indexOps = operations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
    }

    private ProductDocument product(String id, String name, String category,
                                    String brand, String price) {
        ProductDocument doc = new ProductDocument();
        doc.setId(id);
        doc.setName(name);
        doc.setCategory(category);
        doc.setBrand(brand);
        doc.setPrice(new BigDecimal(price));
        return doc;
    }

    @Nested
    @DisplayName("findByCategory")
    class FindByCategoryTests {

        @Test
        @DisplayName("Should return all products for a given category")
        void shouldReturnAllProductsForCategory() {
            Page<ProductDocument> result =
                    repository.findByCategory("Phone", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent())
                    .extracting(ProductDocument::getName)
                    .containsExactlyInAnyOrder("iPhone 15", "Galaxy S24", "Pixel 8");
        }

        @Test
        @DisplayName("Should return a single result for a category with one product")
        void shouldReturnSingleResultForCategoryWithOneProduct() {
            Page<ProductDocument> result =
                    repository.findByCategory("Wearable", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Watch Series 9");
        }

        @Test
        @DisplayName("Should return empty page for a non-existent category")
        void shouldReturnEmptyPageForNonExistentCategory() {
            Page<ProductDocument> result =
                    repository.findByCategory("Spaceship", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasContent()).isFalse();
        }

        @Test
        @DisplayName("Should respect pagination on the first page")
        void shouldRespectPaginationFirstPage() {
            Page<ProductDocument> page0 =
                    repository.findByCategory("Phone", PageRequest.of(0, 2));

            assertThat(page0.getTotalElements()).isEqualTo(3);
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalPages()).isEqualTo(2);
            assertThat(page0.isFirst()).isTrue();
            assertThat(page0.hasNext()).isTrue();
        }

        @Test
        @DisplayName("Should respect pagination on the last page")
        void shouldRespectPaginationLastPage() {
            Page<ProductDocument> page1 =
                    repository.findByCategory("Phone", PageRequest.of(1, 2));

            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.isLast()).isTrue();
            assertThat(page1.hasNext()).isFalse();
        }

        @Test
        @DisplayName("Should return empty content when page index is beyond total pages")
        void shouldReturnEmptyContentWhenPageBeyondTotal() {
            Page<ProductDocument> result =
                    repository.findByCategory("Phone", PageRequest.of(10, 5));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should apply sorting from Pageable")
        void shouldApplySortingFromPageable() {
            Page<ProductDocument> result = repository.findByCategory(
                    "Phone",
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "price"))
            );

            assertThat(result.getContent())
                    .extracting(ProductDocument::getName)
                    .containsExactly("Pixel 8", "Galaxy S24", "iPhone 15");
        }
    }

    @Nested
    @DisplayName("findByBrandIgnoreCase")
    class FindByBrandIgnoreCaseTests {

        @Test
        @DisplayName("Should find products with an exact brand match")
        void shouldFindProductsByExactBrand() {
            Page<ProductDocument> result =
                    repository.findByBrandIgnoreCase("Apple", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(4);
            assertThat(result.getContent())
                    .extracting(ProductDocument::getName)
                    .containsExactlyInAnyOrder(
                            "iPhone 15", "MacBook Pro", "AirPods Pro", "Watch Series 9");
        }

        @Test
        @DisplayName("Should find lowercase stored values with an uppercase query")
        void shouldFindLowercaseStoredValueWithUppercaseQuery() {
            Page<ProductDocument> result =
                    repository.findByBrandIgnoreCase("APPLE", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should find uppercase stored values with a lowercase query")
        void shouldFindUppercaseStoredValueWithLowercaseQuery() {
            Page<ProductDocument> result =
                    repository.findByBrandIgnoreCase("samsung", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent())
                    .extracting(ProductDocument::getName)
                    .containsExactlyInAnyOrder("Galaxy S24", "Buds Pro");
        }

        @Test
        @DisplayName("Should find values with a mixed case query")
        void shouldFindWithMixedCaseQuery() {
            Page<ProductDocument> result =
                    repository.findByBrandIgnoreCase("SaMsUnG", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty page for an unknown brand")
        void shouldReturnEmptyPageForUnknownBrand() {
            Page<ProductDocument> result =
                    repository.findByBrandIgnoreCase("Nokia", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should return a single result for a brand with one product")
        void shouldReturnSingleResultForBrandWithOneProduct() {
            Page<ProductDocument> result =
                    repository.findByBrandIgnoreCase("lenovo", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("ThinkPad X1");
        }

        @Test
        @DisplayName("Should respect pagination on brand search")
        void shouldRespectPaginationForBrandSearch() {
            Page<ProductDocument> page =
                    repository.findByBrandIgnoreCase("apple", PageRequest.of(0, 2));

            assertThat(page.getTotalElements()).isEqualTo(4);
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findByPriceBetween")
    class FindByPriceBetweenTests {

        @Test
        @DisplayName("Should return products within the given price range")
        void shouldFindProductsInPriceRange() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("1000.00"),
                    new BigDecimal("2000.00"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent())
                    .extracting(ProductDocument::getName)
                    .containsExactlyInAnyOrder("iPhone 15", "Galaxy S24", "ThinkPad X1");
        }

        @Test
        @DisplayName("Should include the lower boundary value")
        void shouldIncludeLowerBoundary() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("200.00"),
                    new BigDecimal("250.00"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(ProductDocument::getName)
                    .containsExactlyInAnyOrder("Buds Pro", "AirPods Pro");
        }

        @Test
        @DisplayName("Should include the upper boundary value")
        void shouldIncludeUpperBoundary() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("400.00"),
                    new BigDecimal("500.00"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Watch Series 9");
        }

        @Test
        @DisplayName("Should return exact match when min equals max")
        void shouldReturnExactMatchWhenMinEqualsMax() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("999.99"),
                    new BigDecimal("999.99"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Pixel 8");
        }

        @Test
        @DisplayName("Should return empty page when min is greater than max")
        void shouldReturnEmptyWhenMinGreaterThanMax() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("5000.00"),
                    new BigDecimal("100.00"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should return empty page when no product matches the range")
        void shouldReturnEmptyWhenNoProductInRange() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("10000.00"),
                    new BigDecimal("20000.00"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should return all products for a very wide range")
        void shouldReturnAllProductsForVeryWideRange() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    BigDecimal.ZERO,
                    new BigDecimal("100000.00"),
                    PageRequest.of(0, 20)
            );

            assertThat(result.getTotalElements()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should handle a narrow price range correctly")
        void shouldHandleNarrowRange() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("999.00"),
                    new BigDecimal("1001.00"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Pixel 8");
        }

        @Test
        @DisplayName("Should respect pagination on price range queries")
        void shouldRespectPaginationInPriceRange() {
            Page<ProductDocument> page0 = repository.findByPriceBetween(
                    BigDecimal.ZERO,
                    new BigDecimal("3000.00"),
                    PageRequest.of(0, 3)
            );

            assertThat(page0.getTotalElements()).isEqualTo(8);
            assertThat(page0.getContent()).hasSize(3);
            assertThat(page0.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should sort price range results by price ascending")
        void shouldSortByPriceAscending() {
            Page<ProductDocument> result = repository.findByPriceBetween(
                    new BigDecimal("200.00"),
                    new BigDecimal("600.00"),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "price"))
            );

            assertThat(result.getContent())
                    .extracting(ProductDocument::getName)
                    .containsExactly("Buds Pro", "AirPods Pro", "Watch Series 9");
        }
    }

    @Nested
    @DisplayName("General Repository Behavior")
    class GeneralBehaviorTests {

        @Test
        @DisplayName("Should index all products into Elasticsearch")
        void shouldHaveAllProductsIndexed() {
            assertThat(repository.count()).isEqualTo(8);
        }

        @Test
        @DisplayName("Elasticsearch container should be running")
        void containerShouldBeRunning() {
            assertThat(ELASTICSEARCH.isRunning()).isTrue();
        }
    }
}