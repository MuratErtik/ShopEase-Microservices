package org.n11bootcamp.notificationservice.Listeners;

import static org.junit.jupiter.api.Assertions.*;



import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.notificationservice.dtos.events.OrderConfirmedEvent;
import org.n11bootcamp.notificationservice.services.EmailService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Mock
    private EmailService emailService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Channel channel;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final long DELIVERY_TAG = 1L;
    private static final String IDEMPOTENCY_KEY = "notification:processed:" + ORDER_ID + ":order-confirmed";

    @Test
    @DisplayName("onOrderConfirmed: should send emails and ack when event is processed for the first time")
    void onOrderConfirmed_success() throws IOException {
        // given
        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId(ORDER_ID);
        event.setBuyerEmail("buyer@test.com");
        event.setSellerEmail("seller@test.com");

        when(redisTemplate.hasKey(IDEMPOTENCY_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        notificationEventListener.onOrderConfirmed(event, channel, DELIVERY_TAG);

        // then
        verify(emailService).sendOrderConfirmedToBuyer(event);
        verify(emailService).sendOrderConfirmedToSeller(event);
        verify(valueOperations).set(eq(IDEMPOTENCY_KEY), eq("1"), eq(7L), eq(TimeUnit.DAYS));
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("onOrderConfirmed: should ignore event and ack when it was already processed (Idempotency)")
    void onOrderConfirmed_duplicateEvent_shouldIgnore() throws IOException {
        // given
        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId(ORDER_ID);

        when(redisTemplate.hasKey(IDEMPOTENCY_KEY)).thenReturn(true);

        // when
        notificationEventListener.onOrderConfirmed(event, channel, DELIVERY_TAG);

        // then
        verify(channel).basicAck(DELIVERY_TAG, false); // Mükerrer olsa da ack gönderilir ki kuyruktan silinsin
        verifyNoInteractions(emailService);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("onOrderConfirmed: should only send buyer email if seller email is null")
    void onOrderConfirmed_onlyBuyerEmail() throws IOException {
        // given
        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId(ORDER_ID);
        event.setBuyerEmail("buyer@test.com");
        event.setSellerEmail(null);

        when(redisTemplate.hasKey(IDEMPOTENCY_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        notificationEventListener.onOrderConfirmed(event, channel, DELIVERY_TAG);

        // then
        verify(emailService).sendOrderConfirmedToBuyer(event);
        verify(emailService, never()).sendOrderConfirmedToSeller(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    @DisplayName("onOrderConfirmed: should nack when email service throws exception")
    void onOrderConfirmed_exception_shouldNack() throws IOException {
        // given
        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId(ORDER_ID);
        event.setBuyerEmail("buyer@test.com");

        when(redisTemplate.hasKey(IDEMPOTENCY_KEY)).thenReturn(false);
        doThrow(new RuntimeException("Mail server down")).when(emailService).sendOrderConfirmedToBuyer(any());

        // when
        notificationEventListener.onOrderConfirmed(event, channel, DELIVERY_TAG);

        // then
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        // Redis'e kayıt atılmamalı ki tekrar denenebilsin
        verify(redisTemplate, never()).opsForValue();
    }
}