package org.n11bootcamp.inventoryservice.outboxes.handlers;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.inventoryservice.entities.OutboxEvent;
import org.n11bootcamp.inventoryservice.enums.AggregateType;
import org.n11bootcamp.inventoryservice.enums.TargetSystem;
import org.n11bootcamp.inventoryservice.outboxes.OutboxEventHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartServiceOutboxHandler implements OutboxEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${messaging.exchanges.inventory}")
    private String inventoryExchange;

    @Value("${messaging.routing-keys.stock-updated}")
    private String stockUpdatedRoutingKey;

    @Override
    public TargetSystem getTargetSystem() {
        return TargetSystem.CART_SERVICE;
    }

    @Override
    public void handle(OutboxEvent event) {
        if (event.getAggregateType() != AggregateType.INVENTORY) {
            log.warn("Unhandled aggregateType: {} in CartServiceOutboxHandler",
                    event.getAggregateType());
            return;
        }

        switch (event.getEventType()) {
            case STOCK_UPDATED -> publish(event);
            default -> log.warn("Unhandled eventType: {} in CartServiceOutboxHandler",
                    event.getEventType());
        }
    }

    private void publish(OutboxEvent event) {
        try {
            Object payload = objectMapper.readValue(event.getPayload(), Object.class);
            rabbitTemplate.convertAndSend(inventoryExchange, stockUpdatedRoutingKey, payload);
            log.info("Event published to CartService. eventType={}, aggregateId={}",
                    event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish event to CartService. id={}", event.getId(), e);
            throw new RuntimeException("Failed to publish event to CartService.", e);
        }
    }
}
