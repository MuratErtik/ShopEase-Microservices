package org.n11bootcamp.inventoryservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.inventoryservice.entities.OutboxEvent;
import org.n11bootcamp.inventoryservice.enums.AggregateType;
import org.n11bootcamp.inventoryservice.enums.EventType;
import org.n11bootcamp.inventoryservice.enums.OutboxStatus;
import org.n11bootcamp.inventoryservice.enums.TargetSystem;
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
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setup() {
        outboxEventRepository.deleteAll();
    }



    private OutboxEvent createSampleEvent(OutboxStatus status, LocalDateTime createdAt) {
        return OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .aggregateId(UUID.randomUUID().toString())
                .eventType(EventType.STOCK_RESERVED)
                .payload("{}")
                .status(status)
                .createdAt(createdAt)
                .targetSystem(TargetSystem.ORDER_SERVICE)
                .build();
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatusOrderByCreatedAtAsc: should return events with specific status in chronological order")
    void findByStatus_shouldReturnEventsOrderedByDate() {
        // given
        LocalDateTime now = LocalDateTime.now();

        OutboxEvent olderEvent = createSampleEvent(OutboxStatus.PENDING, now.minusMinutes(10));
        OutboxEvent newerEvent = createSampleEvent(OutboxStatus.PENDING, now.minusMinutes(2));
        OutboxEvent sentEvent = createSampleEvent(OutboxStatus.SENT, now);

        outboxEventRepository.saveAll(List.of(newerEvent, sentEvent, olderEvent));

        // when
        List<OutboxEvent> result = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(olderEvent.getId());
        assertThat(result.get(1).getId()).isEqualTo(newerEvent.getId());
        assertThat(result).allMatch(event -> event.getStatus() == OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("findByStatusOrderByCreatedAtAsc: should return empty list when no events with status exist")
    void findByStatus_returnsEmpty_whenNoMatch() {
        // given
        outboxEventRepository.save(createSampleEvent(OutboxStatus.SENT, LocalDateTime.now()));

        // when
        List<OutboxEvent> result = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save: should persist all fields of outbox event")
    void save_persistsEventCorrectly() {
        // given
        OutboxEvent event = createSampleEvent(OutboxStatus.PENDING, LocalDateTime.now());

        // when
        OutboxEvent saved = outboxEventRepository.save(event);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo(AggregateType.ORDER);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}