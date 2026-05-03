package org.n11bootcamp.inventoryservice.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {
    private UUID orderId;
    private UUID userId;
    private List<OrderConfirmedEventItem> items;
}
