package org.n11bootcamp.paymentservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.paymentservice.entities.Payment;
import org.n11bootcamp.paymentservice.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("payment_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentRepository paymentRepository;

    private static final UUID ORDER_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        paymentRepository.deleteAll();
    }

    private Payment createSamplePayment(UUID orderId, PaymentStatus status) {
        return Payment.builder()
                .orderId(orderId)
                .status(status)
                .amount(new BigDecimal("100.00"))
                .userId(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("existsByOrderId: should return true when payment exists for order")
    void existsByOrderId_returnsTrue_whenExists() {
        paymentRepository.save(createSamplePayment(ORDER_ID, PaymentStatus.COMPLETED));

        boolean exists = paymentRepository.existsByOrderId(ORDER_ID);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("findByOrderId: should return payment when orderId matches")
    void findByOrderId_returnsPayment_whenExists() {
        Payment saved = paymentRepository.save(createSamplePayment(ORDER_ID, PaymentStatus.PENDING));

        Optional<Payment> found = paymentRepository.findByOrderId(ORDER_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(ORDER_ID);
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("findByOrderIdWithLock: should find payment and apply pessimistic lock")
    void findByOrderIdWithLock_returnsPayment() {
        paymentRepository.save(createSamplePayment(ORDER_ID, PaymentStatus.COMPLETED));

        Optional<Payment> found = paymentRepository.findByOrderIdWithLock(ORDER_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("findByOrderIdAndStatus: should return payment when both match")
    void findByOrderIdAndStatus_returnsPayment_whenMatch() {
        paymentRepository.save(createSamplePayment(ORDER_ID, PaymentStatus.COMPLETED));

        Optional<Payment> found = paymentRepository.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.COMPLETED);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("findByOrderIdAndStatus: should return empty when status does not match")
    void findByOrderIdAndStatus_returnsEmpty_whenStatusMismatch() {
        paymentRepository.save(createSamplePayment(ORDER_ID, PaymentStatus.COMPLETED));

        Optional<Payment> found = paymentRepository.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.FAILED);

        assertThat(found).isEmpty();
    }
}