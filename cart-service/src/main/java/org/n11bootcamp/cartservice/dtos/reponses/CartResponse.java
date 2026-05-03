package org.n11bootcamp.cartservice.dtos.reponses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private String userId;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;
    private int totalItems;
}
