package org.n11bootcamp.orderservice.consumers;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.orderservice.dtos.events.*;
import org.n11bootcamp.orderservice.services.impl.OrderServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderServiceImpl orderService;

    @RabbitListener(queues = "${messaging.queues.stock-reserved}")
    public void onStockReserved(
            StockReservedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("StockReservedEvent received. orderId={}", event.getOrderId());
        try {
            orderService.handleStockReserved(event.getOrderId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process StockReservedEvent. orderId={}", event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queues.stock-reservation-failed}")
    public void onStockReservationFailed(
            StockReservationFailedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("StockReservationFailedEvent received. orderId={}", event.getOrderId());
        try {
            orderService.handleStockReservationFailed(event.getOrderId(), event.getReason());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process StockReservationFailedEvent. orderId={}", event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queues.payment-completed}")
    public void onPaymentCompleted(
            PaymentCompletedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("PaymentCompletedEvent received. orderId={}", event.getOrderId());
        try {
            orderService.handlePaymentCompleted(event.getOrderId(),event.getSellerEmail());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent. orderId={}", event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queues.payment-failed}")
    public void onPaymentFailed(
            PaymentFailedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("PaymentFailedEvent received. orderId={}", event.getOrderId());
        try {
            orderService.handlePaymentFailed(event.getOrderId(), event.getReason());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process PaymentFailedEvent. orderId={}", event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
