package org.n11bootcamp.paymentservice.outboxes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.paymentservice.entities.OutboxEvent;
import org.n11bootcamp.paymentservice.enums.OutboxStatus;
import org.n11bootcamp.paymentservice.enums.TargetSystem;
import org.n11bootcamp.paymentservice.repositories.OutboxEventRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorServiceTest {

    @InjectMocks
    private OutboxProcessorService outboxProcessorService;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private Map<TargetSystem, OutboxEventHandler> outboxHandlers;

    @Mock
    private OutboxEventHandler outboxEventHandler;

    @Test
    @DisplayName("Should do nothing when there are no pending events")
    void it_should_do_nothing_when_there_are_no_pending_events() {
        // given
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(Collections.emptyList());

        // when
        outboxProcessorService.processOutbox();

        // then
        verify(outboxRepository, never()).save(any());
        verifyNoInteractions(outboxHandlers);
    }

    @Test
    @DisplayName("Should mark event as SENT when handler processes successfully")
    void it_should_mark_event_as_sent_when_handler_processes_successfully() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .targetSystem(TargetSystem.ORDER_SERVICE)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ORDER_SERVICE)).thenReturn(outboxEventHandler);

        // when
        outboxProcessorService.processOutbox();

        // then
        verify(outboxEventHandler).handle(event);
        then(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("Should mark event as FAILED when no suitable handler is found")
    void it_should_mark_event_as_failed_when_no_handler_found() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .targetSystem(TargetSystem.ORDER_SERVICE)
                .status(OutboxStatus.PENDING)
                .build();

        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ORDER_SERVICE)).thenReturn(null);

        // when
        outboxProcessorService.processOutbox();

        // then
        then(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(outboxRepository).save(event);
        verifyNoInteractions(outboxEventHandler);
    }

    @Test
    @DisplayName("Should increment retry count when handler throws an exception")
    void it_should_increment_retry_count_when_handler_throws_exception() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .targetSystem(TargetSystem.ORDER_SERVICE)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ORDER_SERVICE)).thenReturn(outboxEventHandler);

        doThrow(new RuntimeException("Temporary error")).when(outboxEventHandler).handle(event);

        // when
        outboxProcessorService.processOutbox();

        // then
        then(event.getRetryCount()).isEqualTo(1);
        then(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(outboxRepository).save(event);
    }

    public static Stream<Arguments> retry_limit_provider() {
        return Stream.of(
                Arguments.of(2), // 2 + 1 = 3 (Limit reached)
                Arguments.of(3)  // 3 + 1 = 4 (Limit exceeded)
        );
    }

    @ParameterizedTest
    @MethodSource("retry_limit_provider")
    @DisplayName("Should mark event as FAILED when MAX_RETRY is reached")
    void it_should_mark_as_failed_when_max_retry_reached(int currentRetry) {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .targetSystem(TargetSystem.ORDER_SERVICE)
                .status(OutboxStatus.PENDING)
                .retryCount(currentRetry)
                .build();

        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ORDER_SERVICE)).thenReturn(outboxEventHandler);

        doThrow(new RuntimeException("Persistent error")).when(outboxEventHandler).handle(event);

        // when
        outboxProcessorService.processOutbox();

        // then
        then(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        then(event.getRetryCount()).isGreaterThanOrEqualTo(3);
        verify(outboxRepository).save(event);
    }

    @ParameterizedTest
    @MethodSource("multiple_event_counts")
    @DisplayName("Should process multiple pending events in order")
    void it_should_process_all_pending_events(int count) {
        // given
        List<OutboxEvent> events = Stream.generate(() -> OutboxEvent.builder()
                        .id(UUID.randomUUID())
                        .targetSystem(TargetSystem.ORDER_SERVICE)
                        .status(OutboxStatus.PENDING)
                        .build())
                .limit(count)
                .toList();

        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(events);
        when(outboxHandlers.get(TargetSystem.ORDER_SERVICE)).thenReturn(outboxEventHandler);

        // when
        outboxProcessorService.processOutbox();

        // then
        verify(outboxEventHandler, times(count)).handle(any());
        verify(outboxRepository, times(count)).save(any());
    }

    static Stream<Integer> multiple_event_counts() {
        return Stream.of(1, 3, 5);
    }
}