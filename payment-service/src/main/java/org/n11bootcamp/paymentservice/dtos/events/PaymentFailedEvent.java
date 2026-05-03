package org.n11bootcamp.paymentservice.dtos.events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private UUID orderId;
    private UUID paymentId;
    private UUID userId;
    private String reason;
}
