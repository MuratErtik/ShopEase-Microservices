package org.n11bootcamp.productservice.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.enums.AggregateType;
import org.n11bootcamp.productservice.enums.EventType;
import org.n11bootcamp.productservice.enums.OutboxStatus;
import org.n11bootcamp.productservice.enums.TargetSystem;
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
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setup() {
        outboxEventRepository.deleteAll();
    }

    private OutboxEvent createSampleEvent(OutboxStatus status, LocalDateTime createdAt) {
        return OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .aggregateId(UUID.randomUUID().toString())
                .eventType(EventType.PRODUCT_CREATED)
                .targetSystem(TargetSystem.ELASTICSEARCH)
                .payload("{}")
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatusOrderByCreatedAtAsc: should return events with correct status in ascending order")
    void findByStatus_shouldReturnOrderedEvents() {
        // given
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent olderEvent = createSampleEvent(OutboxStatus.PENDING, now.minusMinutes(10));
        OutboxEvent newerEvent = createSampleEvent(OutboxStatus.PENDING, now.minusMinutes(5));
        OutboxEvent sentEvent = createSampleEvent(OutboxStatus.SENT, now);

        outboxEventRepository.saveAll(List.of(newerEvent, sentEvent, olderEvent));

        // when
        List<OutboxEvent> result = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(olderEvent.getId()); // Önce eski olan gelmeli
        assertThat(result.get(1).getId()).isEqualTo(newerEvent.getId()); // Sonra yeni olan
        assertThat(result).extracting(OutboxEvent::getStatus).containsOnly(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("findByStatusOrderByCreatedAtAsc: should return empty list when no matching status exists")
    void findByStatus_returnsEmpty_whenNoMatch() {
        // given
        outboxEventRepository.save(createSampleEvent(OutboxStatus.SENT, LocalDateTime.now()));

        // when
        List<OutboxEvent> result = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save: should persist outbox event correctly")
    void save_persistsEvent() {
        // given
        OutboxEvent event = createSampleEvent(OutboxStatus.PENDING, LocalDateTime.now());

        // when
        OutboxEvent saved = outboxEventRepository.save(event);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo(AggregateType.PRODUCT);
    }
}