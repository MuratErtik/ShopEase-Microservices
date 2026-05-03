package org.n11bootcamp.orderservice.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.orderservice.clients.CartClient;
import org.n11bootcamp.orderservice.dtos.requests.CreateOrderRequest;
import org.n11bootcamp.orderservice.dtos.responses.CartItemResponse;
import org.n11bootcamp.orderservice.dtos.responses.CartResponse;
import org.n11bootcamp.orderservice.dtos.responses.OrderResponse;
import org.n11bootcamp.orderservice.entities.Order;
import org.n11bootcamp.orderservice.entities.OrderItem;
import org.n11bootcamp.orderservice.entities.OutboxEvent;
import org.n11bootcamp.orderservice.enums.EventType;
import org.n11bootcamp.orderservice.enums.OrderStatus;
import org.n11bootcamp.orderservice.exceptions.DuplicateOrderException;
import org.n11bootcamp.orderservice.exceptions.EmptyCartException;
import org.n11bootcamp.orderservice.exceptions.OrderNotFoundException;
import org.n11bootcamp.orderservice.repositories.OrderRepository;
import org.n11bootcamp.orderservice.repositories.OutboxEventRepository;
import org.n11bootcamp.orderservice.services.impl.OrderServiceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private CartClient cartClient;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private OrderServiceImpl orderService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();

    // --- Create Order Tests ---

    @Test
    @DisplayName("createOrder should save order and save outbox event when request is valid")
    void createOrder_success() throws JsonProcessingException {
        // given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("Istanbul")
                .buyerEmail("murat@ertik.me")
                .cardHolderName("Murat Ertik")
                .cardNumber("1234123412341234")
                .expireMonth("12")
                .expireYear("2028")
                .cvc("123")
                .build();

        CartItemResponse cartItem = new CartItemResponse(
                PRODUCT_ID,
                "Test Product",
                new BigDecimal("100"),
                2,
                "http://image.url",
                SELLER_ID
        );

        CartResponse cartResponse = new CartResponse();
        cartResponse.setUserId(USER_ID);
        cartResponse.setItems(List.of(cartItem));
        cartResponse.setTotalPrice(new BigDecimal("200"));

        when(orderRepository.existsByUserIdAndStatus(USER_ID, OrderStatus.PENDING)).thenReturn(false);
        when(cartClient.getCart(USER_ID.toString())).thenReturn(cartResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Order savedOrder = Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .items(new ArrayList<>())
                .totalAmount(new BigDecimal("200"))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // when
        OrderResponse response = orderService.createOrder(USER_ID, request);

        // then
        assertThat(response.getId()).isEqualTo(ORDER_ID);
        verify(orderRepository).save(any(Order.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
        verify(cartClient).clearCart(USER_ID.toString());
    }

    @Test
    @DisplayName("createOrder should throw DuplicateOrderException when there is already a pending order")
    void createOrder_duplicateOrder() {
        when(orderRepository.existsByUserIdAndStatus(USER_ID, OrderStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, new CreateOrderRequest()))
                .isInstanceOf(DuplicateOrderException.class);
    }

    @Test
    @DisplayName("createOrder should throw EmptyCartException when cart has no items")
    void createOrder_emptyCart() {
        when(orderRepository.existsByUserIdAndStatus(USER_ID, OrderStatus.PENDING)).thenReturn(false);

        CartResponse emptyCart = new CartResponse();
        emptyCart.setItems(new ArrayList<>());
        when(cartClient.getCart(USER_ID.toString())).thenReturn(emptyCart);

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, new CreateOrderRequest()))
                .isInstanceOf(EmptyCartException.class);
    }

    // --- Saga Step: Stock Reserved Tests ---

    @Test
    @DisplayName("handleStockReserved should update status to STOCK_RESERVED and save outbox event")
    void handleStockReserved_success() throws JsonProcessingException {
        // given
        Order order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("200"))
                .build();

        when(orderRepository.findByIdWithLock(ORDER_ID)).thenReturn(Optional.of(order));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        orderService.handleStockReserved(ORDER_ID);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        verify(outboxRepository).save(any(OutboxEvent.class));
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("handleStockReserved should ignore event if order status is not PENDING (Idempotency)")
    void handleStockReserved_idempotent() {
        Order order = Order.builder().id(ORDER_ID).status(OrderStatus.CANCELLED).build();
        when(orderRepository.findByIdWithLock(ORDER_ID)).thenReturn(Optional.of(order));

        orderService.handleStockReserved(ORDER_ID);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(outboxRepository);
    }

    // --- Saga Step: Payment Completed Tests ---

    @Test
    @DisplayName("handlePaymentCompleted should update status to CONFIRMED and notify systems")
    void handlePaymentCompleted_success() throws JsonProcessingException {
        // given
        OrderItem item = OrderItem.builder().totalPrice(new BigDecimal("100")).build();
        Order order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.STOCK_RESERVED)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(orderRepository.findByIdWithLock(ORDER_ID)).thenReturn(Optional.of(order));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        orderService.handlePaymentCompleted(ORDER_ID, "seller@n11.com");

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getSellerEmail()).isEqualTo("seller@n11.com");
        verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
        verify(orderRepository).save(order);
    }

    // --- Saga Step: Payment Failed Tests ---

    @Test
    @DisplayName("handlePaymentFailed should cancel order and request stock release")
    void handlePaymentFailed_success() throws JsonProcessingException {
        // given
        OrderItem item = OrderItem.builder().productId(PRODUCT_ID).quantity(1).build();
        Order order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.STOCK_RESERVED)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(orderRepository.findByIdWithLock(ORDER_ID)).thenReturn(Optional.of(order));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        orderService.handlePaymentFailed(ORDER_ID, "Insufficient funds");

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.STOCK_RELEASED);
        verify(orderRepository).save(order);
    }

    // --- Helper Tests ---

    @Test
    @DisplayName("getOrder should return response when user is authorized")
    void getOrder_success() {
        Order order = Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(ORDER_ID, USER_ID);

        assertThat(response.getId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("getOrder should throw exception when order belongs to different user")
    void getOrder_unauthorized() {
        Order order = Order.builder().id(ORDER_ID).userId(UUID.randomUUID()).build();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(ORDER_ID, USER_ID))
                .isInstanceOf(OrderNotFoundException.class);
    }
}