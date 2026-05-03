package org.n11bootcamp.paymentservice.outboxes;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.paymentservice.entities.OutboxEvent;
import org.n11bootcamp.paymentservice.enums.OutboxStatus;
import org.n11bootcamp.paymentservice.enums.TargetSystem;
import org.n11bootcamp.paymentservice.repositories.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessorService {

    private static final int MAX_RETRY = 3;

    private final OutboxEventRepository outboxRepository;
    private final Map<TargetSystem, OutboxEventHandler> outboxHandlers;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (events.isEmpty()) return;

        log.debug("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            OutboxEventHandler handler = outboxHandlers.get(event.getTargetSystem());

            if (handler == null) {
                log.error("No handler found for targetSystem: {}. id={}",
                        event.getTargetSystem(), event.getId());
                event.setStatus(OutboxStatus.FAILED);
            } else {
                handler.handle(event);
                event.setStatus(OutboxStatus.SENT);
                log.debug("Outbox event sent. id={}", event.getId());
            }

        } catch (Exception e) {
            handleRetry(event, e);
        }

        outboxRepository.save(event);
    }

    private void handleRetry(OutboxEvent event, Exception e) {
        int currentRetry = event.getRetryCount() != null ? event.getRetryCount() : 0;
        event.setRetryCount(currentRetry + 1);

        log.error("Outbox event failed. id={}, retry={}/{}, error={}",
                event.getId(), event.getRetryCount(), MAX_RETRY, e.getMessage());

        if (event.getRetryCount() >= MAX_RETRY) {
            event.setStatus(OutboxStatus.FAILED);
            log.error("Outbox event moved to fail after {} retries. id={}",
                    MAX_RETRY, event.getId());
        }
    }
}