package org.n11bootcamp.inventoryservice.dtos.requests;



import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockRequest {

    @NotNull(message = "Order ID must not be null")
    private UUID orderId;

    @NotNull(message = "Product ID must not be null")
    private UUID productId;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Reservation quantity must be at least 1")
    private Integer quantity;
}