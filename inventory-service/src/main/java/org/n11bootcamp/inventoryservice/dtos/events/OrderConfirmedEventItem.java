package org.n11bootcamp.inventoryservice.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEventItem {
    private UUID productId;
    private Integer quantity;
}
