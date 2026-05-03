package org.n11bootcamp.productservice.outboxes.handlers;

import static org.junit.jupiter.api.Assertions.*;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.productservice.dtos.events.ProductCreatedEventPayload;
import org.n11bootcamp.productservice.entities.OutboxEvent;
import org.n11bootcamp.productservice.enums.AggregateType;
import org.n11bootcamp.productservice.enums.EventType;
import org.n11bootcamp.productservice.enums.TargetSystem;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

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

    private final String EXCHANGE = "product-exchange";
    private final String RK_CREATED = "product-created-key";
    private final String RK_DELETED = "product-deleted-key";

    @BeforeEach
    void setUp() {
        // @Value ile atanan field'ları manuel setliyoruz
        ReflectionTestUtils.setField(inventoryServiceOutboxHandler, "productExchange", EXCHANGE);
        ReflectionTestUtils.setField(inventoryServiceOutboxHandler, "productCreatedRoutingKey", RK_CREATED);
        ReflectionTestUtils.setField(inventoryServiceOutboxHandler, "productDeletedRoutingKey", RK_DELETED);
    }

    @Test
    @DisplayName("Should return INVENTORY_SERVICE as target system")
    void it_should_return_inventory_service_as_target_system() {
        // when
        TargetSystem targetSystem = inventoryServiceOutboxHandler.getTargetSystem();

        // then
        then(targetSystem).isEqualTo(TargetSystem.INVENTORY_SERVICE);
    }

    @Test
    @DisplayName("Should publish ProductCreatedEvent payload when event type is PRODUCT_CREATED")
    void it_should_publish_product_created_event() throws Exception {
        // given
        String payloadJson = "{\"productId\": \"123\"}";
        ProductCreatedEventPayload mockPayload = new ProductCreatedEventPayload();

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .eventType(EventType.PRODUCT_CREATED)
                .payload(payloadJson)
                .aggregateId(UUID.randomUUID().toString())
                .build();

        when(objectMapper.readValue(eq(payloadJson), eq(ProductCreatedEventPayload.class))).thenReturn(mockPayload);

        // when
        inventoryServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, RK_CREATED, mockPayload);
    }

    @Test
    @DisplayName("Should publish aggregateId when event type is PRODUCT_DELETED")
    void it_should_publish_product_deleted_event() {
        // given
        String aggregateId = UUID.randomUUID().toString();
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .eventType(EventType.PRODUCT_DELETED)
                .aggregateId(aggregateId)
                .build();

        // when
        inventoryServiceOutboxHandler.handle(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, RK_DELETED, aggregateId);
        verifyNoInteractions(objectMapper); // Deleted senaryosunda deserialization yapılmıyor
    }

    @Test
    @DisplayName("Should do nothing when aggregate type is not PRODUCT")
    void it_should_do_nothing_when_aggregate_type_is_not_product() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.ORDER)
                .eventType(EventType.PRODUCT_CREATED)
                .build();

        // when
        inventoryServiceOutboxHandler.handle(event);

        // then
        verifyNoInteractions(rabbitTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should log warning and do nothing when event type is unhandled")
    void it_should_do_nothing_when_event_type_is_unhandled() {
        // given
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AggregateType.PRODUCT)
                .eventType(EventType.PRODUCT_UPDATED) // Handler'da case'i yok
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
                .aggregateType(AggregateType.PRODUCT)
                .eventType(EventType.PRODUCT_CREATED)
                .payload("{}")
                .build();

        when(objectMapper.readValue(anyString(), eq(ProductCreatedEventPayload.class)))
                .thenThrow(new RuntimeException("JSON error"));

        // when & then
        assertThatThrownBy(() -> inventoryServiceOutboxHandler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish ProductCreatedEvent.");
    }
}