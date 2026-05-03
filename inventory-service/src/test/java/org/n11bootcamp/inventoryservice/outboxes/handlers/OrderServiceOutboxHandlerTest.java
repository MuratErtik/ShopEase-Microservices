package org.n11bootcamp.inventoryservice.outboxes.handlers;

import static org.junit.jupiter.api.Assertions.*;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
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

    private static final String EXCHANGE = "inventory-exchange";
    private static final String RK_RESERVED = "stock.reserved";
    private static final String RK_RELEASED = "stock.released";
    private static final String RK_FAILED = "stock.reservation.failed";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderServiceOutboxHandler, "inventoryExchange", EXCHANGE);
        ReflectionTestUtils.setField(orderServiceOutboxHandler, "stockReservedRoutingKey", RK_RESERVED);
        ReflectionTestUtils.setField(orderServiceOutboxHandler, "stockReleasedRoutingKey", RK_RELEASED);
        ReflectionTestUtils.setField(orderServiceOutboxHandler, "stockReservationFailedRoutingKey", RK_FAILED);
    }

    @Test
    void it_should_return_order_service_as_target_system() {
        then(orderServiceOutboxHandler.getTargetSystem()).isEqualTo(TargetSystem.ORDER_SERVICE);
    }

    private static Stream<Arguments> successful_event_provider() {
        return Stream.of(
                Arguments.of(EventType.STOCK_RESERVED, RK_RESERVED),
                Arguments.of(EventType.STOCK_RELEASED, RK_RELEASED),
                Arguments.of(EventType.STOCK_RESERVATION_FAILED, RK_FAILED)
        );
    }

    @ParameterizedTest
    @MethodSource("successful_event_provider")
    void it_should_publish_correct_event_to_correct_routing_key(EventType eventType, String expectedRoutingKey) throws Exception {
        // given
        String payloadJson = "{\"orderId\": \"123\"}";
        Object mockPayloadObj = new Object();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.INVENTORY)
                .eventType(eventType)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(Object.class))).thenReturn(mockPayloadObj);

        // when
        orderServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, expectedRoutingKey, mockPayloadObj);
    }

    @Test
    void it_should_do_nothing_when_aggregate_type_is_not_inventory() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.STOCK_RESERVED)
                .build();

        // when
        orderServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate, objectMapper);
    }

    @Test
    void it_should_do_nothing_when_event_type_is_unhandled() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.INVENTORY)
                .eventType(EventType.PRODUCT_DELETED)
                .build();

        // when
        orderServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate, objectMapper);
    }

    @Test
    void it_should_throw_runtime_exception_when_serialization_fails() throws Exception {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.INVENTORY)
                .eventType(EventType.STOCK_RESERVED)
                .payload("invalid-json")
                .build();

        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(new RuntimeException("Jackson error"));

        // when & then
        assertThatThrownBy(() -> orderServiceOutboxHandler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event to OrderService.");
    }
}