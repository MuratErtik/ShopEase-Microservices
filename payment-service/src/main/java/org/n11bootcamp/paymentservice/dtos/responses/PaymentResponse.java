package org.n11bootcamp.paymentservice.dtos.responses;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.n11bootcamp.paymentservice.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String iyzicoPaymentId;
    private String failureReason;
    private String cardHolderName;
    private String cardLastFour;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
