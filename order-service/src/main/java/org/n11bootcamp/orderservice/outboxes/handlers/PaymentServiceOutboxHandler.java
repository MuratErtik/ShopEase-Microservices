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
public class PaymentServiceOutboxHandler implements OutboxEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${messaging.exchanges.order}")
    private String orderExchange;

    @Value("${messaging.routing-keys.payment-requested}")
    private String paymentRequestedRoutingKey;

    @Override
    public TargetSystem getTargetSystem() {
        return TargetSystem.PAYMENT_SERVICE;
    }

    @Override
    public void handle(OutboxEvent event) {
        if (event.getAggregateType() != AggregateType.ORDER) {
            log.warn("Unhandled aggregateType: {} in PaymentServiceOutboxHandler",
                    event.getAggregateType());
            return;
        }

        switch (event.getEventType()) {
            case ORDER_CONFIRMED, PAYMENT_REQUESTED -> publish(event);
            default -> log.warn("Unhandled eventType: {} in PaymentServiceOutboxHandler",
                    event.getEventType());
        }
    }

    private void publish(OutboxEvent event) {
        try {
            Object payload = objectMapper.readValue(event.getPayload(), Object.class);
            rabbitTemplate.convertAndSend(orderExchange, paymentRequestedRoutingKey, payload);
            log.info("PaymentRequestedEvent published. aggregateId={}", event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish PaymentRequestedEvent. id={}", event.getId(), e);
            throw new RuntimeException("Failed to publish PaymentRequestedEvent.", e);
        }
    }
}
