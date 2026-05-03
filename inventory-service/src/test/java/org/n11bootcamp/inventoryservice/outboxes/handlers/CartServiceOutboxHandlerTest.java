package org.n11bootcamp.inventoryservice.outboxes.handlers;

import static org.junit.jupiter.api.Assertions.*;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.inventoryservice.entities.OutboxEvent;
import org.n11bootcamp.inventoryservice.enums.AggregateType;
import org.n11bootcamp.inventoryservice.enums.EventType;
import org.n11bootcamp.inventoryservice.enums.TargetSystem;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceOutboxHandlerTest {

    @InjectMocks
    private CartServiceOutboxHandler cartServiceOutboxHandler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private final String EXCHANGE = "test-exchange";
    private final String ROUTING_KEY = "test-routing-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartServiceOutboxHandler, "inventoryExchange", EXCHANGE);
        ReflectionTestUtils.setField(cartServiceOutboxHandler, "stockUpdatedRoutingKey", ROUTING_KEY);
    }

    @Test
    void it_should_return_cart_service_as_target_system() {
        // when
        TargetSystem targetSystem = cartServiceOutboxHandler.getTargetSystem();

        // then
        then(targetSystem).isEqualTo(TargetSystem.CART_SERVICE);
    }

    @Test
    void it_should_publish_message_when_event_is_stock_updated() throws Exception {
        // given
        String payloadJson = "{\"productId\": \"123\", \"quantity\": 10}";
        Object mockPayloadObj = new Object();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.INVENTORY)
                .eventType(EventType.STOCK_UPDATED)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(Object.class))).thenReturn(mockPayloadObj);

        // when
        cartServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, mockPayloadObj);
    }

    @Test
    void it_should_do_nothing_when_aggregate_type_is_not_inventory() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.STOCK_UPDATED)
                .build();

        // when
        cartServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void it_should_do_nothing_when_event_type_is_not_handled() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.INVENTORY)
                .eventType(EventType.STOCK_RESERVATION_FAILED)
                .build();

        // when
        cartServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void it_should_throw_runtime_exception_when_publishing_fails() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.INVENTORY)
                .eventType(EventType.STOCK_UPDATED)
                .payload("{}")
                .build();

        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(new RuntimeException("JSON Error"));

        // when & then
        assertThatThrownBy(() -> cartServiceOutboxHandler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event to CartService.");
    }
}