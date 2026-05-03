package org.n11bootcamp.orderservice.repositories;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.n11bootcamp.orderservice.entities.OutboxEvent;
import org.n11bootcamp.orderservice.enums.OutboxStatus;
import org.n11bootcamp.orderservice.enums.TargetSystem;
import org.n11bootcamp.orderservice.enums.AggregateType;
import org.n11bootcamp.orderservice.enums.EventType;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxEventRepositoryTest {

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
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager; // Önbelleği yönetmek için eklendi

    @BeforeEach
    void setup() {
        outboxEventRepository.deleteAll();
    }

    private OutboxEvent createEvent(OutboxStatus status, int retryCount) {
        return OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .aggregateId(UUID.randomUUID().toString())
                .eventType(EventType.ORDER_CREATED)
                .targetSystem(TargetSystem.INVENTORY_SERVICE)
                .payload("{}")
                .status(status)
                .retryCount(retryCount)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("markDeadEvents: should update status to FAILED for events exceeding max retry")
    void markDeadEvents_shouldUpdateStatusCorrectly() {
        // given
        int maxRetry = 3;
        OutboxEvent deadEvent = createEvent(OutboxStatus.PENDING, 3);
        OutboxEvent aliveEvent = createEvent(OutboxStatus.PENDING, 1);
        OutboxEvent sentEvent = createEvent(OutboxStatus.SENT, 5);

        outboxEventRepository.saveAll(List.of(deadEvent, aliveEvent, sentEvent));

        // KRİTİK ADIM: Nesneleri DB'ye gönder ve Persistence Context'i boşalt
        entityManager.flush();
        entityManager.clear();

        // when
        int updatedCount = outboxEventRepository.markDeadEvents(maxRetry);

        // then
        assertThat(updatedCount).isEqualTo(1);

        // Artık veritabanından taze veri çekilecek
        OutboxEvent updatedEvent = outboxEventRepository.findById(deadEvent.getId()).orElseThrow();
        assertThat(updatedEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);

        OutboxEvent stillPendingEvent = outboxEventRepository.findById(aliveEvent.getId()).orElseThrow();
        assertThat(stillPendingEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("findTop50ByStatusOrderByCreatedAtAsc: should return events in chronological order")
    void findTop50_shouldReturnLimitedAndOrderedEvents() {
        List<OutboxEvent> events = IntStream.range(0, 60)
                .mapToObj(i -> createEvent(OutboxStatus.PENDING, 0))
                .toList();
        outboxEventRepository.saveAll(events);

        entityManager.flush();
        entityManager.clear();

        List<OutboxEvent> result = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        assertThat(result).hasSize(50);
        assertThat(result.get(0).getCreatedAt()).isBeforeOrEqualTo(result.get(49).getCreatedAt());
    }

    @Test
    @DisplayName("findTop50ByStatusOrderByCreatedAtAsc: should return empty list when no PENDING events exist")
    void findTop50_returnsEmpty_whenNoPending() {
        outboxEventRepository.save(createEvent(OutboxStatus.SENT, 0));

        entityManager.flush();
        entityManager.clear();

        List<OutboxEvent> result = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        assertThat(result).isEmpty();
    }
}