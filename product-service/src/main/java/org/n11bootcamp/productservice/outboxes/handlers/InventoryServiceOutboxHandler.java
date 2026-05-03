package org.n11bootcamp.productservice.outboxes.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.productservice.dtos.events.ProductCreatedEventPayload;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.enums.AggregateType;
import org.n11bootcamp.productservice.enums.TargetSystem;
import org.n11bootcamp.productservice.outboxes.OutboxEventHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryServiceOutboxHandler implements OutboxEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${messaging.exchanges.product}")
    private String productExchange;

    @Value("${messaging.routing-keys.product-created}")
    private String productCreatedRoutingKey;

    @Value("${messaging.routing-keys.product-deleted}")
    private String productDeletedRoutingKey;

    @Override
    public TargetSystem getTargetSystem() {
        return TargetSystem.INVENTORY_SERVICE;
    }

    @Override
    public void handle(OutboxEvent event) {
        if (event.getAggregateType() != AggregateType.PRODUCT) {
            log.warn("Unhandled aggregateType: {} in InventoryServiceOutboxHandler",
                    event.getAggregateType());
            return;
        }

        switch (event.getEventType()) {
            case PRODUCT_CREATED -> publishProductCreated(event);
            case PRODUCT_DELETED -> publishProductDeleted(event);
            default -> log.warn("Unhandled eventType: {} in InventoryServiceOutboxHandler",
                    event.getEventType());
        }
    }

    private void publishProductCreated(OutboxEvent event) {
        try {
            ProductCreatedEventPayload payload = objectMapper.readValue(
                    event.getPayload(), ProductCreatedEventPayload.class);

            rabbitTemplate.convertAndSend(productExchange, productCreatedRoutingKey, payload);

            log.info("ProductCreatedEvent published to InventoryService. productId={}",
                    event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish ProductCreatedEvent. id={}", event.getId(), e);
            throw new RuntimeException("Failed to publish ProductCreatedEvent.", e);
        }
    }

    private void publishProductDeleted(OutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(productExchange, productDeletedRoutingKey,
                    event.getAggregateId());

            log.info("ProductDeletedEvent published to InventoryService. productId={}",
                    event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish ProductDeletedEvent. id={}", event.getId(), e);
            throw new RuntimeException("Failed to publish ProductDeletedEvent.", e);
        }
    }
}