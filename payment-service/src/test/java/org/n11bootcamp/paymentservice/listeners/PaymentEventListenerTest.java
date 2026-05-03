package org.n11bootcamp.paymentservice.listeners;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.paymentservice.dtos.events.PaymentRequestedEvent;
import org.n11bootcamp.paymentservice.services.PaymentService;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    @Mock
    private PaymentService paymentService;

    @Mock
    private Channel channel;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final long DELIVERY_TAG = 1L;

    @Test
    @DisplayName("onPaymentRequested: should process payment and ack when successful")
    void onPaymentRequested_success() throws IOException {
        PaymentRequestedEvent event = new PaymentRequestedEvent();
        event.setOrderId(ORDER_ID);

        paymentEventListener.onPaymentRequested(event, channel, DELIVERY_TAG);

        verify(paymentService).processPayment(event);
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("onPaymentRequested: should nack when payment processing fails")
    void onPaymentRequested_failure() throws IOException {
        PaymentRequestedEvent event = new PaymentRequestedEvent();
        event.setOrderId(ORDER_ID);

        doThrow(new RuntimeException("Payment failed")).when(paymentService).processPayment(event);

        paymentEventListener.onPaymentRequested(event, channel, DELIVERY_TAG);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}