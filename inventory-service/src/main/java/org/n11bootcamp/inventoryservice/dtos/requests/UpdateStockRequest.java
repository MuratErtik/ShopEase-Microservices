package org.n11bootcamp.inventoryservice.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockRequest {

        @NotNull(message = "Quantity must not be null")
        @Min(value = 0, message = "Quantity must not be negative")
        private Integer quantity;
}
