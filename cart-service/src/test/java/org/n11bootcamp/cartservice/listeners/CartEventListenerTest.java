package org.n11bootcamp.cartservice.listeners;

import static org.junit.jupiter.api.Assertions.*;



import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.cartservice.dtos.events.StockUpdatedEvent;
import org.n11bootcamp.cartservice.services.impl.CartServiceImpl;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartEventListenerTest {

    @InjectMocks
    private CartEventListener cartEventListener;

    @Mock
    private CartServiceImpl cartService;

    @Mock
    private Channel channel;

    @Test
    void it_should_handle_stock_update_and_ack_when_successful() throws IOException {
        // given
        UUID productId = new UUID(0, 0);
        Integer newQuantity = 10;
        long deliveryTag = 1L;

        StockUpdatedEvent event = new StockUpdatedEvent();
        event.setProductId(productId);
        event.setNewQuantity(newQuantity);

        // when
        cartEventListener.onStockUpdated(event, channel, deliveryTag);

        // then
        verify(cartService).handleStockUpdate(productId, newQuantity);
        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void it_should_nack_when_exception_occurs() throws IOException {
        // given
        UUID productId = new UUID(0, 0);
        Integer newQuantity = 5;
        long deliveryTag = 2L;

        StockUpdatedEvent event = new StockUpdatedEvent();
        event.setProductId(productId);
        event.setNewQuantity(newQuantity);

        // cartService hata fırlattığında
        doThrow(new RuntimeException("Service error"))
                .when(cartService).handleStockUpdate(productId, newQuantity);

        // when
        cartEventListener.onStockUpdated(event, channel, deliveryTag);

        // then
        verify(cartService).handleStockUpdate(productId, newQuantity);
        verify(channel).basicNack(deliveryTag, false, false); // requeue: false
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}