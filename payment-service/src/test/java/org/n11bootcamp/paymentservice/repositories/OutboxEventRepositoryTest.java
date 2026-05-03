package org.n11bootcamp.paymentservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.paymentservice.entities.OutboxEvent;
import org.n11bootcamp.paymentservice.enums.AggregateType;
import org.n11bootcamp.paymentservice.enums.EventType;
import org.n11bootcamp.paymentservice.enums.OutboxStatus;
import org.n11bootcamp.paymentservice.enums.TargetSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxEventRepositoryTest {

    // ── Infrastructure ─────────────────────────────────────────────────────────

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
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setup() {
        outboxEventRepository.deleteAll();
    }

    // ── Helper Methods ─────────────────────────────────────────────────────────

    private OutboxEvent createEvent(OutboxStatus status, int retryCount, LocalDateTime createdAt) {
        return OutboxEvent.builder()
                .status(status)
                .retryCount(retryCount)
                .createdAt(createdAt)
                .payload("{}")
                .aggregateType(AggregateType.PAYMENT)
                .aggregateId(UUID.randomUUID().toString())
                .eventType(EventType.PAYMENT_COMPLETED)
                .targetSystem(TargetSystem.ORDER_SERVICE)
                .build();
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findTop50ByStatusOrderByCreatedAtAsc: should return events in chronological order")
    void findTop50ByStatusOrderByCreatedAtAsc_returnsOrderedEvents() {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event1 = createEvent(OutboxStatus.PENDING, 0, now.minusMinutes(10));
        OutboxEvent event2 = createEvent(OutboxStatus.PENDING, 0, now.minusMinutes(20)); // En eski
        OutboxEvent event3 = createEvent(OutboxStatus.SENT, 0, now.minusMinutes(5));

        outboxEventRepository.saveAll(List.of(event1, event2, event3));

        List<OutboxEvent> result = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(event2.getId());
        assertThat(result.get(1).getId()).isEqualTo(event1.getId());
    }



    @Test
    @DisplayName("findTop50ByStatusOrderByCreatedAtAsc: should return empty list when no pending events")
    void findTop50ByStatusOrderByCreatedAtAsc_returnsEmpty_whenNoneFound() {
        outboxEventRepository.save(createEvent(OutboxStatus.SENT, 0, LocalDateTime.now()));

        List<OutboxEvent> result = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED);

        assertThat(result).isEmpty();
    }
}