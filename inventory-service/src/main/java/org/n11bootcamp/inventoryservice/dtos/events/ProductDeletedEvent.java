package org.n11bootcamp.inventoryservice.dtos.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeletedEvent {
    private UUID productId;
}