package org.n11bootcamp.orderservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.n11bootcamp.orderservice.configs.RequestContext;
import org.n11bootcamp.orderservice.dtos.requests.CreateOrderRequest;
import org.n11bootcamp.orderservice.dtos.responses.OrderResponse;
import org.n11bootcamp.orderservice.services.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order management and tracking endpoints")
public class OrderController {

    private final OrderService orderService;
    private final RequestContext requestContext;

    @PostMapping
    @Operation(summary = "Create a new order")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid order request")
    @ApiResponse(responseCode = "404", description = "One or more products not found")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = requestContext.getCurrentUserId(httpRequest);
        String buyerEmail = requestContext.getCurrentUserEmail(httpRequest);
        request.setBuyerEmail(buyerEmail);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(userId, request));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details by ID")
    @ApiResponse(responseCode = "200", description = "Order details retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Order does not belong to the current user")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            HttpServletRequest httpRequest) {
        UUID userId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(orderService.getOrder(orderId, userId));
    }

    @GetMapping
    @Operation(summary = "Get current user's order history")
    @ApiResponse(responseCode = "200", description = "List of orders retrieved successfully")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {
        UUID userId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(orderService.getOrders(userId, pageable));
    }

    @GetMapping("/seller")
    @Operation(summary = "Get orders received by the seller")
    @ApiResponse(responseCode = "200", description = "List of seller orders retrieved successfully")
    public ResponseEntity<Page<OrderResponse>> getSellerOrders(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {
        UUID sellerId = requestContext.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(orderService.getOrdersBySeller(sellerId, pageable));
    }
}