package org.n11bootcamp.orderservice.outboxes.handlers;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.orderservice.entities.OutboxEvent;
import org.n11bootcamp.orderservice.enums.AggregateType;
import org.n11bootcamp.orderservice.enums.EventType;
import org.n11bootcamp.orderservice.enums.TargetSystem;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceOutboxHandlerTest {

    @InjectMocks
    private PaymentServiceOutboxHandler paymentServiceOutboxHandler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private final String EXCHANGE = "order-exchange";
    private final String ROUTING_KEY = "payment-requested-key";

    @BeforeEach
    void setUp() {
        // @Value ile atanan field'ları manuel setliyoruz
        ReflectionTestUtils.setField(paymentServiceOutboxHandler, "orderExchange", EXCHANGE);
        ReflectionTestUtils.setField(paymentServiceOutboxHandler, "paymentRequestedRoutingKey", ROUTING_KEY);
    }

    @Test
    @DisplayName("Should return PAYMENT_SERVICE as target system")
    void it_should_return_payment_service_as_target_system() {
        // when
        TargetSystem targetSystem = paymentServiceOutboxHandler.getTargetSystem();

        // then
        then(targetSystem).isEqualTo(TargetSystem.PAYMENT_SERVICE);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"ORDER_CONFIRMED", "PAYMENT_REQUESTED"})
    @DisplayName("Should publish message when event type is ORDER_CONFIRMED or PAYMENT_REQUESTED")
    void it_should_publish_message_for_valid_event_types(EventType eventType) throws Exception {
        // given
        String payloadJson = "{\"orderId\": \"123\", \"totalAmount\": 100.0}";
        Object mockPayloadObj = new Object();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(eventType)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(Object.class))).thenReturn(mockPayloadObj);

        // when
        paymentServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, mockPayloadObj);
    }

    @Test
    @DisplayName("Should do nothing when aggregate type is not ORDER")
    void it_should_do_nothing_when_aggregate_type_is_not_order() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(null) //
                .eventType(EventType.PAYMENT_REQUESTED)
                .build();

        // when
        paymentServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("Should do nothing when event type is not handled")
    void it_should_do_nothing_when_event_type_is_not_handled() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.ORDER_CREATED) // Bu handler tarafından işlenmeyen bir tip
                .build();

        // when
        paymentServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("Should throw runtime exception when publishing fails due to serialization error")
    void it_should_throw_runtime_exception_when_publishing_fails() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.PAYMENT_REQUESTED)
                .payload("{}")
                .build();

        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(new RuntimeException("JSON processing error"));

        // when & then
        assertThatThrownBy(() -> paymentServiceOutboxHandler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish PaymentRequestedEvent.");
    }
}