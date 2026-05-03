package org.n11bootcamp.orderservice.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItemResponse {
    private UUID productId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private UUID sellerId;
}
