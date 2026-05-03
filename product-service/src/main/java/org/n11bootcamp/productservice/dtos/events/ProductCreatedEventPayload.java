package org.n11bootcamp.productservice.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEventPayload {
    private UUID productId;
    private UUID sellerId;
    private String sellerEmail;
    private Integer initialQuantity;
}