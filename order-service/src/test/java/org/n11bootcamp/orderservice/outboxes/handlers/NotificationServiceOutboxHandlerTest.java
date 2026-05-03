package org.n11bootcamp.orderservice.outboxes.handlers;

import static org.junit.jupiter.api.Assertions.*;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class NotificationServiceOutboxHandlerTest {

    @InjectMocks
    private NotificationServiceOutboxHandler notificationServiceOutboxHandler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private final String EXCHANGE = "order-notification-exchange";
    private final String ROUTING_KEY = "order-confirmed-notification-key";

    @BeforeEach
    void setUp() {
        // @Value field'larını ReflectionTestUtils ile setliyoruz
        ReflectionTestUtils.setField(notificationServiceOutboxHandler, "orderExchange", EXCHANGE);
        ReflectionTestUtils.setField(notificationServiceOutboxHandler, "orderConfirmedRoutingKey", ROUTING_KEY);
    }

    @Test
    @DisplayName("Should return NOTIFICATION_SERVICE as target system")
    void it_should_return_notification_service_as_target_system() {
        // when
        TargetSystem targetSystem = notificationServiceOutboxHandler.getTargetSystem();

        // then
        then(targetSystem).isEqualTo(TargetSystem.NOTIFICATION_SERVICE);
    }

    @Test
    @DisplayName("Should publish message when event is ORDER_CONFIRMED")
    void it_should_publish_message_when_event_is_order_confirmed() throws Exception {
        // given
        String payloadJson = "{\"orderId\": \"order-123\", \"buyerEmail\": \"buyer@test.com\"}";
        Object mockPayloadObj = new Object();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.ORDER_CONFIRMED)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(Object.class))).thenReturn(mockPayloadObj);

        // when
        notificationServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, mockPayloadObj);
    }

    @Test
    @DisplayName("Should do nothing when aggregate type is not ORDER")
    void it_should_do_nothing_when_aggregate_type_is_not_order() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(null)
                .eventType(EventType.ORDER_CONFIRMED)
                .build();

        // when
        notificationServiceOutboxHandler.handle(event);

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
                .eventType(EventType.ORDER_CREATED) // Notification handler sadece CONFIRMED işliyor
                .build();

        // when
        notificationServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("Should throw runtime exception when publishing fails")
    void it_should_throw_runtime_exception_when_publishing_fails() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.ORDER_CONFIRMED)
                .payload("{}")
                .build();

        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(new RuntimeException("JSON processing error"));

        // when & then
        assertThatThrownBy(() -> notificationServiceOutboxHandler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event to NotificationService.");
    }
}