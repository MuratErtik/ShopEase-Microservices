package org.n11bootcamp.cartservice.dtos.reponses;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryResponse {
    private UUID id;
    private UUID productId;
    private UUID sellerId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
}