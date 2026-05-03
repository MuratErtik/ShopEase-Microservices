package org.n11bootcamp.paymentservice.outboxes.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.paymentservice.entities.OutboxEvent;
import org.n11bootcamp.paymentservice.enums.AggregateType;
import org.n11bootcamp.paymentservice.enums.EventType;
import org.n11bootcamp.paymentservice.enums.TargetSystem;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceOutboxHandlerTest {

    @InjectMocks
    private OrderServiceOutboxHandler orderServiceOutboxHandler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private final String EXCHANGE = "payment-exchange";
    private final String COMPLETED_ROUTING_KEY = "payment-completed-key";
    private final String FAILED_ROUTING_KEY = "payment-failed-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderServiceOutboxHandler, "paymentExchange", EXCHANGE);
        ReflectionTestUtils.setField(orderServiceOutboxHandler, "paymentCompletedRoutingKey", COMPLETED_ROUTING_KEY);
        ReflectionTestUtils.setField(orderServiceOutboxHandler, "paymentFailedRoutingKey", FAILED_ROUTING_KEY);
    }

    @Test
    void it_should_return_order_service_as_target_system() {
        // when
        TargetSystem targetSystem = orderServiceOutboxHandler.getTargetSystem();

        // then
        then(targetSystem).isEqualTo(TargetSystem.ORDER_SERVICE);
    }

    @Test
    void it_should_publish_message_when_event_is_payment_completed() throws Exception {
        // given
        String payloadJson = "{\"orderId\": \"123\"}";
        Object mockPayloadObj = new Object();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PAYMENT)
                .eventType(EventType.PAYMENT_COMPLETED)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(Object.class))).thenReturn(mockPayloadObj);

        // when
        orderServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, COMPLETED_ROUTING_KEY, mockPayloadObj);
    }

    @Test
    void it_should_publish_message_when_event_is_payment_failed() throws Exception {
        // given
        String payloadJson = "{\"orderId\": \"123\", \"reason\": \"insufficient_funds\"}";
        Object mockPayloadObj = new Object();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PAYMENT)
                .eventType(EventType.PAYMENT_FAILED)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(Object.class))).thenReturn(mockPayloadObj);

        // when
        orderServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, FAILED_ROUTING_KEY, mockPayloadObj);
    }

    @Test
    void it_should_do_nothing_when_aggregate_type_is_not_payment() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(null)
                .eventType(EventType.PAYMENT_COMPLETED)
                .build();

        // when
        orderServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void it_should_throw_runtime_exception_when_publishing_fails() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PAYMENT)
                .eventType(EventType.PAYMENT_COMPLETED)
                .payload("{}")
                .build();

        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(new RuntimeException("Mapping Error"));

        // when & then
        assertThatThrownBy(() -> orderServiceOutboxHandler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event to OrderService.");
    }
}