package org.n11bootcamp.productservice.outboxes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.enums.OutboxStatus;
import org.n11bootcamp.productservice.enums.TargetSystem;
import org.n11bootcamp.productservice.repositories.OutboxEventRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxProcessorServiceTest {

    @InjectMocks
    private OutboxProcessorService outboxProcessorService;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private Map<TargetSystem, OutboxEventHandler> outboxHandlers;

    @Mock
    private OutboxEventHandler outboxEventHandler;

    @Test
    public void it_should_do_nothing_when_there_are_no_pending_events() {

        // given
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of());

        // when
        outboxProcessorService.processOutbox();

        // then
        verify(outboxRepository, never()).save(any());
        verifyNoInteractions(outboxHandlers);
    }

    @Test
    public void it_should_mark_event_as_sent_when_handler_processes_successfully() {

        // given
        OutboxEvent event = OutboxEvent.builder()
                .targetSystem(TargetSystem.ELASTICSEARCH)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ELASTICSEARCH)).thenReturn(outboxEventHandler);

        // when
        outboxProcessorService.processOutbox();

        // then
        verify(outboxEventHandler).handle(event);
        then(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(outboxRepository).save(event);
    }



    public static Stream<Arguments> multiple_pending_events() {
        return Stream.of(
                Arguments.of(2),
                Arguments.of(5),
                Arguments.of(10)
        );
    }

    @ParameterizedTest
    @MethodSource("multiple_pending_events")
    public void it_should_process_all_pending_events(int eventCount) {

        // given
        List<OutboxEvent> events = java.util.Collections.nCopies(eventCount, OutboxEvent.builder()
                .targetSystem(TargetSystem.ELASTICSEARCH)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build());

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(events);
        when(outboxHandlers.get(TargetSystem.ELASTICSEARCH)).thenReturn(outboxEventHandler);

        // when
        outboxProcessorService.processOutbox();

        // then
        verify(outboxEventHandler, times(eventCount)).handle(any());
        verify(outboxRepository, times(eventCount)).save(any());
    }


    @Test
    public void it_should_mark_event_as_failed_when_no_handler_found() {

        // given
        OutboxEvent event = OutboxEvent.builder()
                .targetSystem(TargetSystem.ELASTICSEARCH)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ELASTICSEARCH)).thenReturn(null); // handler yok

        // when
        outboxProcessorService.processOutbox();

        // then
        then(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(outboxRepository).save(event);
        verifyNoInteractions(outboxEventHandler);
    }



    @Test
    public void it_should_increment_retry_count_when_handler_throws_exception() {

        // given
        OutboxEvent event = OutboxEvent.builder()
                .targetSystem(TargetSystem.ELASTICSEARCH)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ELASTICSEARCH)).thenReturn(outboxEventHandler);
        doThrow(new RuntimeException("ES connection failed")).when(outboxEventHandler).handle(event);

        // when
        outboxProcessorService.processOutbox();

        // then
        then(event.getRetryCount()).isEqualTo(1);
        then(event.getStatus()).isNotEqualTo(OutboxStatus.SENT);
        verify(outboxRepository).save(event);
    }



    public static Stream<Arguments> retry_counts_that_should_fail() {
        return Stream.of(
                Arguments.of(2),  // 2 + 1 = 3 → FAILED
                Arguments.of(3),  // 3 + 1 = 4 → FAILED
                Arguments.of(5)   // 5 + 1 = 6 → FAILED
        );
    }

    @ParameterizedTest
    @MethodSource("retry_counts_that_should_fail")
    public void it_should_mark_event_as_failed_when_max_retry_is_reached(int currentRetryCount) {

        // given
        OutboxEvent event = OutboxEvent.builder()
                .targetSystem(TargetSystem.ELASTICSEARCH)
                .status(OutboxStatus.PENDING)
                .retryCount(currentRetryCount)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ELASTICSEARCH)).thenReturn(outboxEventHandler);
        doThrow(new RuntimeException("ES connection failed")).when(outboxEventHandler).handle(event);

        // when
        outboxProcessorService.processOutbox();

        // then
        then(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        then(event.getRetryCount()).isGreaterThanOrEqualTo(3);
        verify(outboxRepository).save(event);
    }


    @Test
    public void it_should_not_mark_event_as_failed_when_max_retry_is_not_reached() {

        // given
        OutboxEvent event = OutboxEvent.builder()
                .targetSystem(TargetSystem.ELASTICSEARCH)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxHandlers.get(TargetSystem.ELASTICSEARCH)).thenReturn(outboxEventHandler);
        doThrow(new RuntimeException("ES connection failed")).when(outboxEventHandler).handle(event);

        // when
        outboxProcessorService.processOutbox();

        // then
        then(event.getStatus()).isNotEqualTo(OutboxStatus.FAILED);
        then(event.getRetryCount()).isEqualTo(1);
        verify(outboxRepository).save(event);
    }
}