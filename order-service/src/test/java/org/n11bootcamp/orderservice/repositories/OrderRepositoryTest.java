package org.n11bootcamp.orderservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.orderservice.entities.Order;
import org.n11bootcamp.orderservice.entities.OrderItem;
import org.n11bootcamp.orderservice.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
    }

    private Order createSampleOrder(UUID userId, OrderStatus status) {
        Order order = Order.builder()
                .userId(userId)
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("Istanbul, Turkey")
                .buyerEmail("buyer@example.com")
                .sellerEmail("seller@example.com")
                .cardHolderName("Murat Ertik")
                .cardNumber("1234567812345678")
                .expireMonth("12")
                .expireYear("2028")
                .cvc("123")
                .build();

        OrderItem item = OrderItem.builder()
                .productId(UUID.randomUUID())
                .sellerId(SELLER_ID)
                .productName("Test Product")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .totalPrice(new BigDecimal("100.00"))
                .order(order)
                .build();

        order.setItems(List.of(item));
        return order;
    }

    @Test
    @DisplayName("findAllByUserId: should return paginated orders")
    void findAllByUserId_returnsPaginatedOrders() {
        orderRepository.save(createSampleOrder(USER_ID, OrderStatus.PENDING));
        orderRepository.save(createSampleOrder(USER_ID, OrderStatus.CONFIRMED));

        Page<Order> result = orderRepository.findAllByUserId(USER_ID, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("existsByUserIdAndStatus: should return true when exists")
    void existsByUserIdAndStatus_returnsTrue_whenExists() {
        orderRepository.save(createSampleOrder(USER_ID, OrderStatus.PENDING));

        boolean exists = orderRepository.existsByUserIdAndStatus(USER_ID, OrderStatus.PENDING);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("findByIdAndStatus: should return order when status matches")
    void findByIdAndStatus_returnsOrder_whenMatches() {
        Order saved = orderRepository.save(createSampleOrder(USER_ID, OrderStatus.CONFIRMED));

        Optional<Order> found = orderRepository.findByIdAndStatus(saved.getId(), OrderStatus.CONFIRMED);

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByIdWithLock: should fetch items with lock")
    void findByIdWithLock_fetchesOrderAndItems() {
        Order saved = orderRepository.save(createSampleOrder(USER_ID, OrderStatus.PENDING));

        Optional<Order> found = orderRepository.findByIdWithLock(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).isNotEmpty();
    }

    @Test
    @DisplayName("findOrdersBySellerIdInItems: should return orders for seller")
    void findOrdersBySellerIdInItems_returnsCorrectOrders() {
        orderRepository.save(createSampleOrder(USER_ID, OrderStatus.PENDING));

        Page<Order> result = orderRepository.findOrdersBySellerIdInItems(SELLER_ID, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("existsByUserIdAndStatus: should return false when status mismatch")
    void existsByUserIdAndStatus_returnsFalse_whenStatusMismatch() {
        orderRepository.save(createSampleOrder(USER_ID, OrderStatus.CONFIRMED));

        boolean exists = orderRepository.existsByUserIdAndStatus(USER_ID, OrderStatus.PENDING);

        assertThat(exists).isFalse();
    }
}