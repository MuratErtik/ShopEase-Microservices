package org.n11bootcamp.paymentservice.listeners;



import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.paymentservice.dtos.events.PaymentRequestedEvent;
import org.n11bootcamp.paymentservice.services.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = "${messaging.queues.payment-requested}")
    public void onPaymentRequested(
            PaymentRequestedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("PaymentRequestedEvent received. orderId={}", event.getOrderId());
        try {
            paymentService.processPayment(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process PaymentRequestedEvent. orderId={}",
                    event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}