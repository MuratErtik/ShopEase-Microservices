package org.n11bootcamp.inventoryservice.rabbitmq.listeners;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.inventoryservice.dtos.events.*;
import org.n11bootcamp.inventoryservice.services.InventoryService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @InjectMocks
    private InventoryEventListener inventoryEventListener;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private Channel channel;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final long DELIVERY_TAG = 1L;


    @Test
    @DisplayName("onProductCreated: should create inventory and ack when successful")
    void onProductCreated_success() throws IOException {
        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setProductId(PRODUCT_ID);
        event.setSellerId(SELLER_ID);
        event.setSellerEmail("seller@n11.com");
        event.setInitialQuantity(100);

        inventoryEventListener.onProductCreated(event, channel, DELIVERY_TAG);

        verify(inventoryService).createInventory(PRODUCT_ID, SELLER_ID, "seller@n11.com", 100);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }


    @Test
    @DisplayName("onProductDeleted: should delete and ack when successful")
    void onProductDeleted_success() throws IOException {
        inventoryEventListener.onProductDeleted(PRODUCT_ID.toString(), channel, DELIVERY_TAG);

        verify(inventoryService).deleteInventory(PRODUCT_ID);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }


    @Test
    @DisplayName("onOrderCreated: should reserve stock for each item and ack")
    void onOrderCreated_success() throws IOException {
        OrderCreatedEventItem item = new OrderCreatedEventItem();
        item.setProductId(PRODUCT_ID);
        item.setQuantity(5);

        OrderCreatedEvent event = new OrderCreatedEvent(ORDER_ID, USER_ID, List.of(item), new BigDecimal("100.00"));

        inventoryEventListener.onOrderCreated(event, channel, DELIVERY_TAG);

        verify(inventoryService).reserveStock(argThat(request ->
                request.getOrderId().equals(ORDER_ID) &&
                        request.getProductId().equals(PRODUCT_ID) &&
                        request.getQuantity().equals(5)
        ));
        verify(channel).basicAck(DELIVERY_TAG, false);
    }


    @Test
    @DisplayName("onOrderConfirmed: should confirm stock for each item and ack")
    void onOrderConfirmed_success() throws IOException {
        OrderConfirmedEventItem item = new OrderConfirmedEventItem();
        item.setProductId(PRODUCT_ID);
        item.setQuantity(3);

        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId(ORDER_ID);
        event.setItems(List.of(item));

        inventoryEventListener.onOrderConfirmed(event, channel, DELIVERY_TAG);

        verify(inventoryService).confirmStock(PRODUCT_ID, ORDER_ID, 3);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }


    @Test
    @DisplayName("onOrderCancelled: should release stock for each item and ack")
    void onOrderCancelled_success() throws IOException {
        OrderCancelledEventItem item = new OrderCancelledEventItem();
        item.setProductId(PRODUCT_ID);
        item.setQuantity(2);

        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(ORDER_ID);
        event.setItems(List.of(item));

        inventoryEventListener.onOrderCancelled(event, channel, DELIVERY_TAG);

        verify(inventoryService).releaseStock(argThat(request ->
                request.getOrderId().equals(ORDER_ID) &&
                        request.getProductId().equals(PRODUCT_ID) &&
                        request.getQuantity().equals(2)
        ));
        verify(channel).basicAck(DELIVERY_TAG, false);
    }


    @Test
    @DisplayName("should nack when any service throws exception")
    void should_nack_when_exception_occurs() throws IOException {
        inventoryEventListener.onProductDeleted(PRODUCT_ID.toString(), channel, DELIVERY_TAG);

        doThrow(new RuntimeException("Error")).when(inventoryService).deleteInventory(any());

        inventoryEventListener.onProductDeleted(PRODUCT_ID.toString(), channel, DELIVERY_TAG);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }
}