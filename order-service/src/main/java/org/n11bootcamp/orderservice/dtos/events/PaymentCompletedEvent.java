package org.n11bootcamp.orderservice.dtos.events;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private UUID orderId;
    private UUID paymentId;
    private String SellerEmail;
    private BigDecimal amount;
}
