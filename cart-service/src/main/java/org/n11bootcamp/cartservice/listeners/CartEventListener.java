package org.n11bootcamp.cartservice.listeners;



import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.n11bootcamp.cartservice.dtos.events.StockUpdatedEvent;
import org.n11bootcamp.cartservice.services.impl.CartServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventListener {

    private final CartServiceImpl cartService;

    @RabbitListener(queues = "${messaging.queues.stock-updated}")
    public void onStockUpdated(
            StockUpdatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("StockUpdatedEvent received. productId={}, newQuantity={}",
                event.getProductId(), event.getNewQuantity());
        try {
            cartService.handleStockUpdate(event.getProductId(), event.getNewQuantity());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process StockUpdatedEvent. productId={}",
                    event.getProductId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
