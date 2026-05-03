package org.n11bootcamp.inventoryservice.dtos.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {
    private UUID productId;
    private UUID sellerId;
    private String sellerEmail;
    private Integer initialQuantity;
}