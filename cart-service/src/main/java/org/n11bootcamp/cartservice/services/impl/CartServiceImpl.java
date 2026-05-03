package org.n11bootcamp.cartservice.services.impl;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.n11bootcamp.cartservice.clients.InventoryClient;
import org.n11bootcamp.cartservice.clients.ProductClient;
import org.n11bootcamp.cartservice.dtos.reponses.*;
import org.n11bootcamp.cartservice.dtos.requests.*;
import org.n11bootcamp.cartservice.exceptions.*;
import org.n11bootcamp.cartservice.models.Cart;
import org.n11bootcamp.cartservice.models.CartItem;
import org.n11bootcamp.cartservice.repositories.CartRepository;
import org.n11bootcamp.cartservice.services.CartService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final InventoryClient inventoryClient;
    private final ProductClient productClient;

    @Override
    public Cart getCart(UUID userId) {
        List<CartItem> items = cartRepository.findAllItems(userId);
        return Cart.of(userId, items);
    }

    @Override
    public Cart addItem(UUID userId, AddCartItemRequest request) {
        UUID productId = request.getProductId();

        ProductResponse product = productClient.getProduct(productId);

        InventoryResponse inventory = inventoryClient.getInventory(productId);

        CartItem existing = cartRepository.findItem(userId, productId);
        int currentQuantityInCart = existing != null ? existing.getQuantity() : 0;
        int totalRequested = currentQuantityInCart + request.getQuantity();

        if (inventory.getAvailableQuantity() < totalRequested) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: %d, Requested: %d"
                            .formatted(inventory.getAvailableQuantity(), totalRequested));
        }

        CartItem itemToSave = CartItem.builder()
                .productId(productId)
                .name(product.getName())
                .price(product.getPrice())
                .quantity(totalRequested)
                .imageUrl(product.getImageUrl())
                .sellerId(product.getSellerId())
                .build();

        cartRepository.saveItem(userId, itemToSave);
        log.info("Item added to cart. userId={}, productId={}, quantity={}",
                userId, productId, totalRequested);

        return getCart(userId);
    }

    @Override
    public Cart updateItem(UUID userId, UUID productId, UpdateCartItemRequest request) {
        CartItem existing = cartRepository.findItem(userId, productId);
        if (existing == null) {
            throw new CartItemNotFoundException(
                    "Cart item not found. productId: " + productId);
        }

        if (request.getQuantity() > existing.getQuantity()) {
            InventoryResponse inventory = inventoryClient.getInventory(productId);
            if (inventory.getAvailableQuantity() < request.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock. Available: %d, Requested: %d"
                                .formatted(inventory.getAvailableQuantity(), request.getQuantity()));
            }
        }

        ProductResponse product = productClient.getProduct(productId);

        CartItem updated = CartItem.builder()
                .productId(productId)
                .name(product.getName())
                .price(product.getPrice())
                .quantity(request.getQuantity())
                .sellerId(product.getSellerId())
                .imageUrl(product.getImageUrl())
                .build();

        cartRepository.saveItem(userId, updated);
        log.info("Cart item updated. userId={}, productId={}, newQuantity={}",
                userId, productId, request.getQuantity());

        return getCart(userId);
    }

    @Override
    public void removeItem(UUID userId, UUID productId) {
        if (!cartRepository.itemExists(userId, productId)) {
            throw new CartItemNotFoundException(
                    "Cart item not found. productId: " + productId);
        }
        cartRepository.removeItem(userId, productId);
        log.info("Item removed from cart. userId={}, productId={}", userId, productId);
    }

    @Override
    public void clearCart(UUID userId) {
        cartRepository.clearCart(userId);
        log.info("Cart cleared. userId={}", userId);
    }

    //redis scan able to add
    public void handleStockUpdate(UUID productId, Integer newQuantity) {
        log.debug("Handling stock update. productId={}, newQuantity={}", productId, newQuantity);

    }
}
