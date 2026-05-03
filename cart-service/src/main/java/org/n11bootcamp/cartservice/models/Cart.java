package org.n11bootcamp.cartservice.models;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    private UUID userId;
    private List<CartItem> items;
    private BigDecimal totalPrice;
    private Integer totalItems;

    public static Cart of(UUID userId, List<CartItem> items) {
        BigDecimal totalPrice = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return Cart.builder()
                .userId(userId)
                .items(items)
                .totalPrice(totalPrice)
                .totalItems(totalItems)
                .build();
    }
}
