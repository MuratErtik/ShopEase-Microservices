package org.n11bootcamp.paymentservice.outboxes.handlers;



import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.paymentservice.entities.OutboxEvent;
import org.n11bootcamp.paymentservice.enums.AggregateType;
import org.n11bootcamp.paymentservice.enums.TargetSystem;
import org.n11bootcamp.paymentservice.outboxes.OutboxEventHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceOutboxHandler implements OutboxEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${messaging.exchanges.payment}")
    private String paymentExchange;

    @Value("${messaging.routing-keys.payment-completed}")
    private String paymentCompletedRoutingKey;

    @Value("${messaging.routing-keys.payment-failed}")
    private String paymentFailedRoutingKey;

    @Override
    public TargetSystem getTargetSystem() {
        return TargetSystem.ORDER_SERVICE;
    }

    @Override
    public void handle(OutboxEvent event) {
        if (event.getAggregateType() != AggregateType.PAYMENT) {
            log.warn("Unhandled aggregateType: {} in OrderServiceOutboxHandler",
                    event.getAggregateType());
            return;
        }

        switch (event.getEventType()) {
            case PAYMENT_COMPLETED -> publish(event, paymentCompletedRoutingKey);
            case PAYMENT_FAILED -> publish(event, paymentFailedRoutingKey);
            default -> log.warn("Unhandled eventType: {} in OrderServiceOutboxHandler",
                    event.getEventType());
        }
    }

    private void publish(OutboxEvent event, String routingKey) {
        try {
            Object payload = objectMapper.readValue(event.getPayload(), Object.class);
            rabbitTemplate.convertAndSend(paymentExchange, routingKey, payload);
            log.info("Event published to OrderService. eventType={}, aggregateId={}",
                    event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish event to OrderService. id={}", event.getId(), e);
            throw new RuntimeException("Failed to publish event to OrderService.", e);
        }
    }
}
