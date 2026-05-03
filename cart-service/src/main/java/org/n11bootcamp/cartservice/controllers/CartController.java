package org.n11bootcamp.cartservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.n11bootcamp.cartservice.configs.RequestContext;
import org.n11bootcamp.cartservice.dtos.requests.*;
import org.n11bootcamp.cartservice.models.Cart;
import org.n11bootcamp.cartservice.services.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;
    private final RequestContext requestContext;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    public ResponseEntity<Cart> getCart(HttpServletRequest request) {
        UUID userId = requestContext.getCurrentUserId(request);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    @ApiResponse(responseCode = "200", description = "Item added to cart successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<Cart> addItem(
            @Valid @RequestBody AddCartItemRequest body,
            HttpServletRequest request) {
        UUID userId = requestContext.getCurrentUserId(request);
        return ResponseEntity.ok(cartService.addItem(userId, body));
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update item quantity in the cart")
    @ApiResponse(responseCode = "200", description = "Cart item updated successfully")
    @ApiResponse(responseCode = "404", description = "Product not found in cart")
    public ResponseEntity<Cart> updateItem(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest body,
            HttpServletRequest request) {
        UUID userId = requestContext.getCurrentUserId(request);
        return ResponseEntity.ok(cartService.updateItem(userId, productId, body));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove an item from the cart")
    @ApiResponse(responseCode = "204", description = "Item removed from cart successfully")
    @ApiResponse(responseCode = "404", description = "Product not found in cart")
    public ResponseEntity<Void> removeItem(
            @PathVariable UUID productId,
            HttpServletRequest request) {
        UUID userId = requestContext.getCurrentUserId(request);
        cartService.removeItem(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear all items from the cart")
    @ApiResponse(responseCode = "204", description = "Cart cleared successfully")
    public ResponseEntity<Void> clearCart(HttpServletRequest request) {
        UUID userId = requestContext.getCurrentUserId(request);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}