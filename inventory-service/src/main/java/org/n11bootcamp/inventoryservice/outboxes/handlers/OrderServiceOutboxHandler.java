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
public class OrderServiceOutboxHandler implements OutboxEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${messaging.exchanges.inventory}")
    private String inventoryExchange;

    @Value("${messaging.routing-keys.stock-reserved}")
    private String stockReservedRoutingKey;

    @Value("${messaging.routing-keys.stock-released}")
    private String stockReleasedRoutingKey;

    @Value("${messaging.routing-keys.stock-reservation-failed}")
    private String stockReservationFailedRoutingKey;

    @Override
    public TargetSystem getTargetSystem() {
        return TargetSystem.ORDER_SERVICE;
    }

    @Override
    public void handle(OutboxEvent event) {
        if (event.getAggregateType() != AggregateType.INVENTORY) {
            log.warn("Unhandled aggregateType: {} in OrderServiceOutboxHandler",
                    event.getAggregateType());
            return;
        }

        switch (event.getEventType()) {
            case STOCK_RESERVED -> publish(event, stockReservedRoutingKey);
            case STOCK_RELEASED -> publish(event, stockReleasedRoutingKey);
            case STOCK_RESERVATION_FAILED -> publish(event, stockReservationFailedRoutingKey);
            default -> log.warn("Unhandled eventType: {} in OrderServiceOutboxHandler",
                    event.getEventType());
        }
    }

    private void publish(OutboxEvent event, String routingKey) {
        try {
            Object payload = objectMapper.readValue(event.getPayload(), Object.class);
            rabbitTemplate.convertAndSend(inventoryExchange, routingKey, payload);
            log.info("Event published to OrderService. eventType={}, aggregateId={}",
                    event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish event to OrderService. id={}", event.getId(), e);
            throw new RuntimeException("Failed to publish event to OrderService.", e);
        }
    }
}
