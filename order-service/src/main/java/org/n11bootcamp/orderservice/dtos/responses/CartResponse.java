package org.n11bootcamp.orderservice.dtos.responses;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartResponse {
    private UUID userId;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;
    private Integer totalItems;
}
