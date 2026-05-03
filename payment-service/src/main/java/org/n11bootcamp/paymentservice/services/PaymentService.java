package org.n11bootcamp.paymentservice.services;



import org.n11bootcamp.paymentservice.dtos.responses.PaymentResponse;
import org.n11bootcamp.paymentservice.dtos.events.PaymentRequestedEvent;

import java.util.UUID;

public interface PaymentService {

    void processPayment(PaymentRequestedEvent event);

    PaymentResponse getByOrderId(UUID orderId);
}
