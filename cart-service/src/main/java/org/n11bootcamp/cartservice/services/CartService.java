package org.n11bootcamp.cartservice.services;



import org.n11bootcamp.cartservice.dtos.requests.AddCartItemRequest;
import org.n11bootcamp.cartservice.dtos.requests.UpdateCartItemRequest;
import org.n11bootcamp.cartservice.models.Cart;

import java.util.UUID;

public interface CartService {

    Cart getCart(UUID userId);

    Cart addItem(UUID userId, AddCartItemRequest request);

    Cart updateItem(UUID userId, UUID productId, UpdateCartItemRequest request);

    void removeItem(UUID userId, UUID productId);

    void clearCart(UUID userId);
}
