package org.n11bootcamp.notificationservice.Listeners;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.notificationservice.dtos.events.OrderConfirmedEvent;
import org.n11bootcamp.notificationservice.services.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PROCESSED_KEY_PREFIX = "notification:processed:";
    private static final long TTL_DAYS = 7;

    @RabbitListener(queues = "${messaging.queues.order-confirmed}")
    public void onOrderConfirmed(
            OrderConfirmedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("OrderConfirmedEvent received. orderId={}", event.getOrderId());

        String idempotencyKey = PROCESSED_KEY_PREFIX + event.getOrderId() + ":order-confirmed";


        if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey))) {
            log.warn("Duplicate OrderConfirmedEvent ignored. orderId={}", event.getOrderId());
            channel.basicAck(deliveryTag, false);  //for idempotency
            return;
        }

        try {
            if (event.getBuyerEmail() != null) {
                emailService.sendOrderConfirmedToBuyer(event);
            }

            if (event.getSellerEmail() != null) {
                emailService.sendOrderConfirmedToSeller(event);
            }


            //point as eventConsumed once
            redisTemplate.opsForValue().set(idempotencyKey, "1", TTL_DAYS, TimeUnit.DAYS);

            channel.basicAck(deliveryTag, false);
            log.info("OrderConfirmedEvent processed. orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process OrderConfirmedEvent. orderId={}", event.getOrderId(), e);
            // dont take in queue before send it to DLQ
            channel.basicNack(deliveryTag, false, false);
        }
    }
}