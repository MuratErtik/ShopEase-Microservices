package org.n11bootcamp.orderservice.outboxes.handlers;

import static org.junit.jupiter.api.Assertions.*;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceOutboxHandlerTest {

    @InjectMocks
    private InventoryServiceOutboxHandler inventoryServiceOutboxHandler;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private static final String EXCHANGE = "order-exchange";
    private static final String RK_CREATED = "order-created-key";
    private static final String RK_CONFIRMED = "order-confirmed-key";
    private static final String RK_RELEASED = "stock-released-key";

    @BeforeEach
    void setUp() {
        // @Value field enjeksiyonlarını manuel yapıyoruz
        ReflectionTestUtils.setField(inventoryServiceOutboxHandler, "orderExchange", EXCHANGE);
        ReflectionTestUtils.setField(inventoryServiceOutboxHandler, "orderCreatedRoutingKey", RK_CREATED);
        ReflectionTestUtils.setField(inventoryServiceOutboxHandler, "orderConfirmedRoutingKey", RK_CONFIRMED);
        ReflectionTestUtils.setField(inventoryServiceOutboxHandler, "stockReleasedRoutingKey", RK_RELEASED);
    }

    @Test
    @DisplayName("Should return INVENTORY_SERVICE as target system")
    void it_should_return_inventory_service_as_target_system() {
        // when
        TargetSystem targetSystem = inventoryServiceOutboxHandler.getTargetSystem();

        // then
        then(targetSystem).isEqualTo(TargetSystem.INVENTORY_SERVICE);
    }

    private static Stream<Arguments> inventory_event_provider() {
        return Stream.of(
                Arguments.of(EventType.ORDER_CREATED, RK_CREATED),
                Arguments.of(EventType.ORDER_CONFIRMED, RK_CONFIRMED),
                Arguments.of(EventType.STOCK_RELEASED, RK_RELEASED)
        );
    }

    @ParameterizedTest
    @MethodSource("inventory_event_provider")
    @DisplayName("Should publish message to correct routing key based on event type")
    void it_should_publish_message_to_correct_routing_key(EventType eventType, String expectedRoutingKey) throws Exception {
        // given
        String payloadJson = "{\"orderId\": \"test-id\"}";
        Object mockPayloadObj = new Object();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(eventType)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(Object.class))).thenReturn(mockPayloadObj);

        // when
        inventoryServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, expectedRoutingKey, mockPayloadObj);
    }

    @Test
    @DisplayName("Should do nothing when aggregate type is not ORDER")
    void it_should_do_nothing_when_aggregate_type_is_not_order() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(null)
                .eventType(EventType.ORDER_CREATED)
                .build();

        // when
        inventoryServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should do nothing when event type is not handled")
    void it_should_do_nothing_when_event_type_is_not_handled() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.PAYMENT_REQUESTED) // Inventory handler bu eventi işlemiyor
                .build();

        // when
        inventoryServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should throw runtime exception when publishing fails")
    void it_should_throw_runtime_exception_when_publishing_fails() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.ORDER_CREATED)
                .payload("{}")
                .build();

        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(new RuntimeException("JSON error"));

        // when & then
        assertThatThrownBy(() -> inventoryServiceOutboxHandler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event to InventoryService.");
    }
}