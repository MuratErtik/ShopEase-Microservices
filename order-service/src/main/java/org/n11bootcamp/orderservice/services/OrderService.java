package org.n11bootcamp.orderservice.services;



import org.n11bootcamp.orderservice.dtos.requests.CreateOrderRequest;
import org.n11bootcamp.orderservice.dtos.responses.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(UUID userId, CreateOrderRequest request);

    OrderResponse getOrder(UUID orderId, UUID userId);

    Page<OrderResponse> getOrders(UUID userId, Pageable pageable);

    Page<OrderResponse> getOrdersBySeller(UUID sellerId, Pageable pageable);
}
