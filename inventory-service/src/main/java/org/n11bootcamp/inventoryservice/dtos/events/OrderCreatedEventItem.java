package org.n11bootcamp.inventoryservice.dtos.events;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEventItem {
    private UUID productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
