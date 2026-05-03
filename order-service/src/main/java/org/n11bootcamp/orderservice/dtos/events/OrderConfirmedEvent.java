package org.n11bootcamp.orderservice.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {
    private UUID orderId;
    private UUID userId;
    private String buyerEmail;
    private String sellerEmail;
    private List<OrderConfirmedEventItem> items;
    private BigDecimal totalAmount;
}