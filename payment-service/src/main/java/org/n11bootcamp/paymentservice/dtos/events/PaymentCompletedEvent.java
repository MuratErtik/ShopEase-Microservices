package org.n11bootcamp.paymentservice.dtos.events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private UUID orderId;
    private UUID paymentId;
    private UUID userId;
    private BigDecimal amount;
}
