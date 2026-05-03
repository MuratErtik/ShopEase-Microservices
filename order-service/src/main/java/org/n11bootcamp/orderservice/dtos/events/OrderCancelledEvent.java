package org.n11bootcamp.orderservice.dtos.events;

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
public class OrderCancelledEvent {
    private UUID orderId;
    private UUID userId;
    private List<OrderCancelledEventItem> items;
    private String reason;
}
