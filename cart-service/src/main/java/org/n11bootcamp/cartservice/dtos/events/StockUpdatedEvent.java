package org.n11bootcamp.cartservice.dtos.events;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdatedEvent {
    private UUID productId;
    private UUID sellerId;
    private Integer newQuantity;
}