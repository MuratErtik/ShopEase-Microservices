package org.n11bootcamp.paymentservice.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestedEvent {
    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private String cardHolderName;
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cvc;
    private String shippingAddress;
}
