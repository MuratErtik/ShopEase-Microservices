package org.n11bootcamp.notificationservice.dtos.events;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderConfirmedEvent {
    private UUID orderId;
    private UUID userId;
    private String buyerEmail;
    private String sellerEmail;
    private BigDecimal totalAmount;
    private List<OrderConfirmedEventItem> items;
}
