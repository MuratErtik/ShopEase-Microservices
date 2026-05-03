package org.n11bootcamp.orderservice.consumers;

import static org.junit.jupiter.api.Assertions.*;


import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.orderservice.dtos.events.*;
import org.n11bootcamp.orderservice.services.impl.OrderServiceImpl;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Mock
    private OrderServiceImpl orderService;

    @Mock
    private Channel channel;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final long DELIVERY_TAG = 123L;

    // --- Stock Reserved Tests ---

    @Test
    @DisplayName("onStockReserved: should call service and ack when successful")
    void onStockReserved_success() throws IOException {
        // given
        StockReservedEvent event = new StockReservedEvent();
        event.setOrderId(ORDER_ID);

        // when
        orderEventConsumer.onStockReserved(event, channel, DELIVERY_TAG);

        // then
        verify(orderService).handleStockReserved(ORDER_ID);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("onStockReserved: should nack when service throws exception")
    void onStockReserved_exception_shouldNack() throws IOException {
        // given
        StockReservedEvent event = new StockReservedEvent();
        event.setOrderId(ORDER_ID);
        doThrow(new RuntimeException("DB Error")).when(orderService).handleStockReserved(any());

        // when
        orderEventConsumer.onStockReserved(event, channel, DELIVERY_TAG);

        // then
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // --- Stock Reservation Failed Tests ---

    @Test
    @DisplayName("onStockReservationFailed: should handle failure and ack")
    void onStockReservationFailed_success() throws IOException {
        // given
        String reason = "Insufficient stock";
        StockReservationFailedEvent event = new StockReservationFailedEvent();
        event.setOrderId(ORDER_ID);
        event.setReason(reason);

        // when
        orderEventConsumer.onStockReservationFailed(event, channel, DELIVERY_TAG);

        // then
        verify(orderService).handleStockReservationFailed(ORDER_ID, reason);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    // --- Payment Completed Tests ---

    @Test
    @DisplayName("onPaymentCompleted: should complete order process and ack")
    void onPaymentCompleted_success() throws IOException {
        // given
        String sellerEmail = "seller@n11.com";
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(ORDER_ID);
        event.setSellerEmail(sellerEmail);

        // when
        orderEventConsumer.onPaymentCompleted(event, channel, DELIVERY_TAG);

        // then
        verify(orderService).handlePaymentCompleted(ORDER_ID, sellerEmail);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    // --- Payment Failed Tests ---

    @Test
    @DisplayName("onPaymentFailed: should handle payment failure and ack")
    void onPaymentFailed_success() throws IOException {
        // given
        String reason = "Card limit exceeded";
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setOrderId(ORDER_ID);
        event.setReason(reason);

        // when
        orderEventConsumer.onPaymentFailed(event, channel, DELIVERY_TAG);

        // then
        verify(orderService).handlePaymentFailed(ORDER_ID, reason);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("onPaymentFailed: should nack when service throws exception")
    void onPaymentFailed_exception_shouldNack() throws IOException {
        // given
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setOrderId(ORDER_ID);
        doThrow(new RuntimeException("System error")).when(orderService).handlePaymentFailed(any(), any());

        // when
        orderEventConsumer.onPaymentFailed(event, channel, DELIVERY_TAG);

        // then
        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }
}