package org.n11bootcamp.inventoryservice.rabbitmq.listeners;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.inventoryservice.dtos.events.*;
import org.n11bootcamp.inventoryservice.dtos.requests.ReserveStockRequest;
import org.n11bootcamp.inventoryservice.dtos.requests.ReleaseStockRequest;
import org.n11bootcamp.inventoryservice.services.InventoryService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryService inventoryService;

    @RabbitListener(queues = "${messaging.queues.product-created}")
    public void onProductCreated(
            ProductCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("ProductCreatedEvent received. productId={}", event.getProductId());
        try {
            inventoryService.createInventory(
                    event.getProductId(),
                    event.getSellerId(),
                    event.getSellerEmail(),
                    event.getInitialQuantity()
            );
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process ProductCreatedEvent. productId={}", event.getProductId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queues.product-deleted}")
    public void onProductDeleted(
            String productIdStr,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("ProductDeletedEvent received. productId={}", productIdStr);
        try {
            UUID productId = UUID.fromString(productIdStr);
            inventoryService.deleteInventory(productId);
            channel.basicAck(deliveryTag, false);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in ProductDeletedEvent. payload={}", productIdStr, e);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("Failed to process ProductDeletedEvent. productId={}", productIdStr, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queues.order-created}")
    public void onOrderCreated(
            OrderCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("OrderCreatedEvent received. orderId={}", event.getOrderId());
        try {
            for (OrderCreatedEventItem item : event.getItems()) {
                ReserveStockRequest request = ReserveStockRequest.builder()
                        .orderId(event.getOrderId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build();
                inventoryService.reserveStock(request);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent. orderId={}", event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queues.order-confirmed}")
    public void onOrderConfirmed(
            OrderConfirmedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("OrderConfirmedEvent received. orderId={}", event.getOrderId());
        try {
            for (OrderConfirmedEventItem item : event.getItems()) {
                inventoryService.confirmStock(item.getProductId(),event.getOrderId(),item.getQuantity());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process OrderConfirmedEvent. orderId={}", event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queues.order-cancelled}")
    public void onOrderCancelled(
            OrderCancelledEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("OrderCancelledEvent received. orderId={}", event.getOrderId());
        try {
            for (OrderCancelledEventItem item : event.getItems()) {
                ReleaseStockRequest request = ReleaseStockRequest.builder()
                        .orderId(event.getOrderId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build();
                inventoryService.releaseStock(request);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process OrderCancelledEvent. orderId={}", event.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}