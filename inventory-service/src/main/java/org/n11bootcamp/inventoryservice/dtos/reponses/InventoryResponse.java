package org.n11bootcamp.inventoryservice.dtos.reponses;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private UUID id;
    private UUID productId;
    private UUID sellerId;
    private String sellerEmail;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
    private LocalDateTime updatedAt;
}
