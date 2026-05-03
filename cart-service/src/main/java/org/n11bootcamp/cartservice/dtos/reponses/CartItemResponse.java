package org.n11bootcamp.cartservice.dtos.reponses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String productId;
    private String name;
    private BigDecimal price;
    private int quantity;
    private String imageUrl;
    private BigDecimal subtotal; // price * quantity
}