package org.n11bootcamp.orderservice.outboxes.handlers;



import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.orderservice.entities.OutboxEvent;
import org.n11bootcamp.orderservice.enums.AggregateType;
import org.n11bootcamp.orderservice.enums.TargetSystem;
import org.n11bootcamp.orderservice.outboxes.OutboxEventHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryServiceOutboxHandler implements OutboxEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${messaging.exchanges.order}")
    private String orderExchange;

    @Value("${messaging.routing-keys.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${messaging.routing-keys.order-confirmed}")
    private String orderConfirmedRoutingKey;

    @Value("${messaging.routing-keys.stock-released}")
    private String stockReleasedRoutingKey;

    @Override
    public TargetSystem getTargetSystem() {
        return TargetSystem.INVENTORY_SERVICE;
    }

    @Override
    public void handle(OutboxEvent event) {
        if (event.getAggregateType() != AggregateType.ORDER) {
            log.warn("Unhandled aggregateType: {} in InventoryServiceOutboxHandler",
                    event.getAggregateType());
            return;
        }

        switch (event.getEventType()) {
            case ORDER_CREATED -> publish(event, orderCreatedRoutingKey);
            case ORDER_CONFIRMED -> publish(event, orderConfirmedRoutingKey);
            case STOCK_RELEASED -> publish(event, stockReleasedRoutingKey);
            default -> log.warn("Unhandled eventType: {} in InventoryServiceOutboxHandler",
                    event.getEventType());
        }
    }

    private void publish(OutboxEvent event, String routingKey) {
        try {
            Object payload = objectMapper.readValue(event.getPayload(), Object.class);
            rabbitTemplate.convertAndSend(orderExchange, routingKey, payload);
            log.info("Event published to InventoryService. eventType={}, aggregateId={}",
                    event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish event to InventoryService. id={}", event.getId(), e);
            throw new RuntimeException("Failed to publish event to InventoryService.", e);
        }
    }
}
