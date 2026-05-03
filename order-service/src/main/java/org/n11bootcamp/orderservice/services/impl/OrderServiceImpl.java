package org.n11bootcamp.orderservice.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.orderservice.clients.CartClient;
import org.n11bootcamp.orderservice.dtos.events.*;
import org.n11bootcamp.orderservice.dtos.responses.*;

import org.n11bootcamp.orderservice.dtos.requests.*;
import org.n11bootcamp.orderservice.entities.Order;
import org.n11bootcamp.orderservice.entities.OrderItem;
import org.n11bootcamp.orderservice.entities.OutboxEvent;
import org.n11bootcamp.orderservice.enums.AggregateType;
import org.n11bootcamp.orderservice.enums.EventType;
import org.n11bootcamp.orderservice.enums.OrderStatus;
import org.n11bootcamp.orderservice.enums.OutboxStatus;
import org.n11bootcamp.orderservice.enums.TargetSystem;

import org.n11bootcamp.orderservice.exceptions.*;

import org.n11bootcamp.orderservice.repositories.OrderRepository;
import org.n11bootcamp.orderservice.repositories.OutboxEventRepository;
import org.n11bootcamp.orderservice.services.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final CartClient cartClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {

        boolean hasPendingOrder = orderRepository
                .existsByUserIdAndStatus(userId, OrderStatus.PENDING);

        if (hasPendingOrder) {
            throw new DuplicateOrderException(
                    "You already have a pending order. Please wait for it to complete.");
        }


        CartResponse cart = cartClient.getCart(userId.toString());

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot create order with empty cart.");
        }


        List<OrderItem> orderItems = cart.getItems().stream()
                .map(this::toOrderItem)
                .toList();

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .cardHolderName(request.getCardHolderName())
                .cardNumber(request.getCardNumber())
                .buyerEmail(request.getBuyerEmail())
                .expireMonth(request.getExpireMonth())
                .expireYear(request.getExpireYear())
                .cvc(request.getCvc())
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.getItems().addAll(orderItems);

        Order saved = orderRepository.save(order);

        OrderCreatedEvent createdEvent = OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .userId(userId)
                .items(orderItems.stream()
                        .map(item -> OrderCreatedEventItem.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .toList())
                .totalAmount(totalAmount)
                .build();

        saveOutboxEvent(
                saved.getId().toString(),
                EventType.ORDER_CREATED,
                TargetSystem.INVENTORY_SERVICE,
                createdEvent
        );

        try {
            cartClient.clearCart(userId.toString());
            log.info("Cart cleared after order creation. userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear cart after order creation. userId={}, error={}",
                    userId, e.getMessage());
        }

        log.info("Order created. orderId={}, userId={}, totalAmount={}",
                saved.getId(), userId, totalAmount);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found. orderId: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Order not found. orderId: " + orderId);
        }

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(UUID userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void handleStockReserved(UUID orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found. orderId: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("StockReservedEvent ignored. orderId={}, currentStatus={}",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.STOCK_RESERVED);
        order.setSellerEmail(order.getSellerEmail());

        // Outbox — PaymentRequestedEvent to PaymentService
        PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .cardHolderName(order.getCardHolderName())
                .cardNumber(order.getCardNumber())
                .expireMonth(order.getExpireMonth())
                .expireYear(order.getExpireYear())
                .cvc(order.getCvc())
                .build();

        saveOutboxEvent(
                order.getId().toString(),
                EventType.PAYMENT_REQUESTED,
                TargetSystem.PAYMENT_SERVICE,
                paymentEvent
        );

        //for security
        order.setCardHolderName(null);
        order.setCardNumber(null);
        order.setExpireMonth(null);
        order.setExpireYear(null);
        order.setCvc(null);

        orderRepository.save(order);

        log.info("Order status updated to STOCK_RESERVED. orderId={}", orderId);
    }

    @Transactional
    public void handleStockReservationFailed(UUID orderId, String reason) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found. orderId: " + orderId));

        // Idempotency
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("StockReservationFailedEvent ignored. orderId={}, currentStatus={}",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order cancelled due to stock reservation failure. orderId={}, reason={}",
                orderId, reason);
    }

    @Transactional
    public void handlePaymentCompleted(UUID orderId,String sellerEmail) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found. orderId: " + orderId));

        if (order.getStatus() != OrderStatus.STOCK_RESERVED) {
            log.warn("PaymentCompletedEvent ignored. orderId={}, currentStatus={}",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setSellerEmail(sellerEmail);
        orderRepository.save(order);

        OrderConfirmedEvent confirmedEvent = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .buyerEmail(order.getBuyerEmail())
                .sellerEmail(order.getSellerEmail())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream()
                        .map(item -> OrderConfirmedEventItem.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .toList())
                .build();

        saveOutboxEvent(
                order.getId().toString(),
                EventType.ORDER_CONFIRMED,
                TargetSystem.INVENTORY_SERVICE,
                confirmedEvent
        );

        saveOutboxEvent(
                order.getId().toString(),
                EventType.ORDER_CONFIRMED,
                TargetSystem.NOTIFICATION_SERVICE,
                confirmedEvent
        );

        log.info("Order confirmed. orderId={}", orderId);
    }



    @Transactional
    public void handlePaymentFailed(UUID orderId, String reason) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found. orderId: " + orderId));

        // Idempotency kontrolü
        if (order.getStatus() != OrderStatus.STOCK_RESERVED) {
            log.warn("PaymentFailedEvent ignored. orderId={}, currentStatus={}",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Outbox — StockReleasedEvent → InventoryService
        OrderCancelledEvent cancelledEvent = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(item -> OrderCancelledEventItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .reason(reason)
                .build();

        saveOutboxEvent(
                order.getId().toString(),
                EventType.STOCK_RELEASED,
                TargetSystem.INVENTORY_SERVICE,
                cancelledEvent
        );

        log.info("Order cancelled due to payment failure. orderId={}, reason={}",
                orderId, reason);
    }


    private OrderItem toOrderItem(CartItemResponse cartItem) {
        BigDecimal totalPrice = cartItem.getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return OrderItem.builder()
                .productId(cartItem.getProductId())
                .productName(cartItem.getName())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getPrice())
                .totalPrice(totalPrice)
                .sellerId(cartItem.getSellerId())
                .build();
    }

    private void saveOutboxEvent(String aggregateId, EventType eventType,
                                 TargetSystem targetSystem, Object payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AggregateType.ORDER)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .targetSystem(targetSystem)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxRepository.save(event);
            log.debug("Outbox event saved. aggregateId={}, eventType={}, target={}",
                    aggregateId, eventType, targetSystem);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload.", e);
        }
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersBySeller(UUID sellerId, Pageable pageable) {
        return orderRepository.findOrdersBySellerIdInItems(sellerId, pageable)
                .map(this::toResponse);
    }
}