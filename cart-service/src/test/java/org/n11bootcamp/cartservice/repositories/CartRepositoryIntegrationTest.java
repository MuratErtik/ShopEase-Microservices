package org.n11bootcamp.cartservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.cartservice.clients.InventoryClient;
import org.n11bootcamp.cartservice.clients.ProductClient;
import org.n11bootcamp.cartservice.models.CartItem;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class CartRepositoryIntegrationTest {

    // ── infrastructure ─────────────────────────────────────────────────────────

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    // Prevent Spring Boot from connecting to a real RabbitMQ broker:
    // @MockBean overrides the auto-configured ConnectionFactory so AMQP
    // infrastructure gets a mock and no TCP connection is ever attempted.
    @MockitoBean
    private ConnectionFactory rabbitConnectionFactory;

    // Feign clients need stubs so the context starts without reaching real services.
    @MockitoBean
    private ProductClient  productClient;
    @MockitoBean
    private InventoryClient inventoryClient;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Redis — dynamic port assigned by Testcontainers
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port",
                () -> redis.getMappedPort(6379).toString());

        // AMQP — values required by RabbitMQConfig @Value fields
        registry.add("messaging.exchanges.inventory",        () -> "test-inventory-exchange");
        registry.add("messaging.queues.stock-updated",       () -> "test-stock-queue");
        registry.add("messaging.routing-keys.stock-updated", () -> "inventory.stock.updated");

        // Prevent AMQP listener containers from trying to connect on startup
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("spring.rabbitmq.listener.direct.auto-startup", () -> "false");

        // Feign base URLs — must be set even though clients are mocked
        registry.add("feign.clients.product-service.url",   () -> "http://localhost:9999");
        registry.add("feign.clients.inventory-service.url", () -> "http://localhost:9999");

        // JWT decoder is initialised lazily; a placeholder URI prevents binding errors
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:9999/.well-known/jwks.json");
    }

    @Autowired private CartRepository               cartRepository;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    // ── shared test data ───────────────────────────────────────────────────────

    private static final UUID USER_ID    = UUID.fromString("eeee0000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_1  = UUID.fromString("ffff0000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_2  = UUID.fromString("ffff0000-0000-0000-0000-000000000002");
    private static final UUID SELLER_ID  = UUID.fromString("aaaa0000-0000-0000-0000-000000000099");

    private CartItem item(UUID productId, String name, BigDecimal price, int qty) {
        return CartItem.builder()
                .productId(productId)
                .name(name)
                .price(price)
                .quantity(qty)
                .imageUrl("http://img.test/" + productId)
                .sellerId(SELLER_ID)
                .build();
    }

    @BeforeEach
    void clearState() {
        // Ensure each test starts with a completely empty cart for USER_ID
        cartRepository.clearCart(USER_ID);
    }

    // ── saveItem / findItem ────────────────────────────────────────────────────

    @Test
    @DisplayName("saveItem + findItem: should round-trip a CartItem through Redis faithfully")
    void saveAndFindItem_roundTrip() {
        CartItem original = item(PRODUCT_1, "Headphones", new BigDecimal("129.99"), 2);

        cartRepository.saveItem(USER_ID, original);
        CartItem loaded = cartRepository.findItem(USER_ID, PRODUCT_1);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getProductId()).isEqualTo(PRODUCT_1);
        assertThat(loaded.getName()).isEqualTo("Headphones");
        assertThat(loaded.getQuantity()).isEqualTo(2);
        assertThat(loaded.getSellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    @DisplayName("saveItem + findItem: should preserve BigDecimal price precision through serialization")
    void saveItem_preservesBigDecimalPrecision() {
        CartItem original = item(PRODUCT_1, "Camera", new BigDecimal("1299.95"), 1);

        cartRepository.saveItem(USER_ID, original);
        CartItem loaded = cartRepository.findItem(USER_ID, PRODUCT_1);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getPrice()).isEqualByComparingTo("1299.95");
    }

    @Test
    @DisplayName("saveItem: should overwrite the existing entry when the same productId is saved again")
    void saveItem_overwritesExistingEntry() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "Widget", new BigDecimal("9.99"), 1));
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "Widget", new BigDecimal("9.99"), 5));

        CartItem loaded = cartRepository.findItem(USER_ID, PRODUCT_1);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("saveItem: should set a positive TTL on the Redis key")
    void saveItem_setsTtlOnKey() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "Keyboard", new BigDecimal("49.99"), 1));

        Long ttl = redisTemplate.getExpire("cart:" + USER_ID);
        assertThat(ttl).isNotNull().isGreaterThan(0L);
    }

    // ── findItem – not found ───────────────────────────────────────────────────

    @Test
    @DisplayName("findItem: should return null when the product is not in the cart")
    void findItem_returnsNull_whenProductAbsent() {
        UUID unknownProduct = UUID.randomUUID();
        assertThat(cartRepository.findItem(USER_ID, unknownProduct)).isNull();
    }

    @Test
    @DisplayName("findItem: should return null when the cart does not exist at all")
    void findItem_returnsNull_whenCartDoesNotExist() {
        UUID newUser = UUID.randomUUID();
        assertThat(cartRepository.findItem(newUser, PRODUCT_1)).isNull();
    }

    // ── findAllItems ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllItems: should return all stored items for a user")
    void findAllItems_returnsAllItems_whenMultipleProductsSaved() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "Item A", new BigDecimal("10.00"), 1));
        cartRepository.saveItem(USER_ID, item(PRODUCT_2, "Item B", new BigDecimal("20.00"), 3));

        List<CartItem> items = cartRepository.findAllItems(USER_ID);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(CartItem::getProductId)
                .containsExactlyInAnyOrder(PRODUCT_1, PRODUCT_2);
    }

    @Test
    @DisplayName("findAllItems: should return an empty list when the cart is empty")
    void findAllItems_returnsEmptyList_whenCartEmpty() {
        assertThat(cartRepository.findAllItems(USER_ID)).isEmpty();
    }

    // ── removeItem ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeItem: should delete only the targeted product from the cart")
    void removeItem_deletesOnlyTargetProduct() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "A", BigDecimal.ONE, 1));
        cartRepository.saveItem(USER_ID, item(PRODUCT_2, "B", BigDecimal.TEN, 2));

        cartRepository.removeItem(USER_ID, PRODUCT_1);

        assertThat(cartRepository.findItem(USER_ID, PRODUCT_1)).isNull();
        assertThat(cartRepository.findItem(USER_ID, PRODUCT_2)).isNotNull();
    }

    @Test
    @DisplayName("removeItem: should be a no-op when the product is not in the cart")
    void removeItem_isNoOp_whenProductAbsent() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_2, "B", BigDecimal.TEN, 1));

        cartRepository.removeItem(USER_ID, PRODUCT_1); // PRODUCT_1 not in cart

        assertThat(cartRepository.findAllItems(USER_ID)).hasSize(1);
    }

    // ── clearCart ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("clearCart: should remove all items and the Redis key for the user")
    void clearCart_removesAllItemsAndKey() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "A", BigDecimal.ONE,  1));
        cartRepository.saveItem(USER_ID, item(PRODUCT_2, "B", BigDecimal.TEN, 2));

        cartRepository.clearCart(USER_ID);

        assertThat(cartRepository.findAllItems(USER_ID)).isEmpty();
        assertThat(cartRepository.cartExists(USER_ID)).isFalse();
    }

    // ── cartExists ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cartExists: should return true when at least one item is present")
    void cartExists_returnsTrue_whenItemPresent() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "X", BigDecimal.ONE, 1));
        assertThat(cartRepository.cartExists(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("cartExists: should return false when the cart has been cleared")
    void cartExists_returnsFalse_afterClear() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "X", BigDecimal.ONE, 1));
        cartRepository.clearCart(USER_ID);
        assertThat(cartRepository.cartExists(USER_ID)).isFalse();
    }

    @Test
    @DisplayName("cartExists: should return false for a user who has never had a cart")
    void cartExists_returnsFalse_forNewUser() {
        assertThat(cartRepository.cartExists(UUID.randomUUID())).isFalse();
    }

    // ── itemExists ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("itemExists: should return true when the product is in the cart")
    void itemExists_returnsTrue_whenProductSaved() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "Y", BigDecimal.TEN, 1));
        assertThat(cartRepository.itemExists(USER_ID, PRODUCT_1)).isTrue();
    }

    @Test
    @DisplayName("itemExists: should return false when the product has not been added")
    void itemExists_returnsFalse_whenProductAbsent() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_2, "Z", BigDecimal.TEN, 1));
        assertThat(cartRepository.itemExists(USER_ID, PRODUCT_1)).isFalse();
    }

    @Test
    @DisplayName("itemExists: should return false after the item has been removed")
    void itemExists_returnsFalse_afterRemove() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "Y", BigDecimal.ONE, 1));
        cartRepository.removeItem(USER_ID, PRODUCT_1);
        assertThat(cartRepository.itemExists(USER_ID, PRODUCT_1)).isFalse();
    }

    // ── getItemCount ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemCount: should return zero for an empty cart")
    void getItemCount_returnsZero_whenEmpty() {
        assertThat(cartRepository.getItemCount(USER_ID)).isZero();
    }

    @Test
    @DisplayName("getItemCount: should reflect the number of distinct products in the cart")
    void getItemCount_returnsDistinctProductCount() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "A", BigDecimal.ONE,  2));
        cartRepository.saveItem(USER_ID, item(PRODUCT_2, "B", BigDecimal.TEN, 5));

        // Two distinct products, regardless of quantity
        assertThat(cartRepository.getItemCount(USER_ID)).isEqualTo(2);
    }

    @Test
    @DisplayName("getItemCount: should decrement when an item is removed")
    void getItemCount_decrements_afterRemove() {
        cartRepository.saveItem(USER_ID, item(PRODUCT_1, "A", BigDecimal.ONE, 1));
        cartRepository.saveItem(USER_ID, item(PRODUCT_2, "B", BigDecimal.TEN, 1));

        cartRepository.removeItem(USER_ID, PRODUCT_1);

        assertThat(cartRepository.getItemCount(USER_ID)).isEqualTo(1);
    }
}