package org.n11bootcamp.paymentservice.services.impl;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyzipay.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.paymentservice.dtos.events.PaymentRequestedEvent;
import org.n11bootcamp.paymentservice.dtos.events.*;

import org.n11bootcamp.paymentservice.dtos.responses.PaymentResponse;
import org.n11bootcamp.paymentservice.entities.OutboxEvent;
import org.n11bootcamp.paymentservice.enums.AggregateType;
import org.n11bootcamp.paymentservice.enums.EventType;
import org.n11bootcamp.paymentservice.enums.OutboxStatus;
import org.n11bootcamp.paymentservice.enums.PaymentStatus;
import org.n11bootcamp.paymentservice.enums.TargetSystem;
import org.n11bootcamp.paymentservice.exceptions.PaymentNotFoundException;
import org.n11bootcamp.paymentservice.repositories.OutboxEventRepository;
import org.n11bootcamp.paymentservice.repositories.PaymentRepository;
import org.n11bootcamp.paymentservice.services.IyzicoPaymentService;
import org.n11bootcamp.paymentservice.services.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final IyzicoPaymentService iyzicoPaymentService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processPayment(PaymentRequestedEvent event) {

        // Idempotency
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Payment already processed for orderId={}. Ignoring duplicate event.",
                    event.getOrderId());
            return;
        }


        org.n11bootcamp.paymentservice.entities.Payment payment =
                org.n11bootcamp.paymentservice.entities.Payment.builder()
                        .orderId(event.getOrderId())
                        .userId(event.getUserId())
                        .amount(event.getTotalAmount())
                        .status(PaymentStatus.PENDING)
                        .cardHolderName(event.getCardHolderName())
                        .cardLastFour(iyzicoPaymentService.extractLastFour(event.getCardNumber()))
                        .build();

        paymentRepository.save(payment);

        try {
            // sending iyzico
            Payment iyzicoResponse = iyzicoPaymentService.charge(event);

            if (iyzicoPaymentService.isSuccess(iyzicoResponse)) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setIyzicoPaymentId(iyzicoResponse.getPaymentId());
                paymentRepository.save(payment);

                PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                        .orderId(event.getOrderId())
                        .paymentId(payment.getId())
                        .userId(event.getUserId())
                        .amount(event.getTotalAmount())
                        .build();

                saveOutboxEvent(
                        payment.getId().toString(),
                        EventType.PAYMENT_COMPLETED,
                        TargetSystem.ORDER_SERVICE,
                        completedEvent
                );

                log.info("Payment completed. orderId={}, paymentId={}, iyzicoPaymentId={}",
                        event.getOrderId(), payment.getId(), iyzicoResponse.getPaymentId());

            } else {
                String failureReason = iyzicoPaymentService.extractFailureReason(iyzicoResponse);

                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(failureReason);
                paymentRepository.save(payment);

                PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                        .orderId(event.getOrderId())
                        .paymentId(payment.getId())
                        .userId(event.getUserId())
                        .reason(failureReason)
                        .build();

                saveOutboxEvent(
                        payment.getId().toString(),
                        EventType.PAYMENT_FAILED,
                        TargetSystem.ORDER_SERVICE,
                        failedEvent
                );

                log.warn("Payment failed. orderId={}, reason={}", event.getOrderId(), failureReason);
            }

        } catch (Exception e) {
            String errorMessage = "iyzico connection error: " + e.getMessage();

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(errorMessage);
            paymentRepository.save(payment);

            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .paymentId(payment.getId())
                    .userId(event.getUserId())
                    .reason(errorMessage)
                    .build();

            saveOutboxEvent(
                    payment.getId().toString(),
                    EventType.PAYMENT_FAILED,
                    TargetSystem.ORDER_SERVICE,
                    failedEvent
            );

            log.error("Unexpected error during payment processing. orderId={}",
                    event.getOrderId(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        org.n11bootcamp.paymentservice.entities.Payment payment =
                paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new PaymentNotFoundException(
                                "Payment not found for orderId: " + orderId));

        return toResponse(payment);
    }

    private void saveOutboxEvent(String aggregateId, EventType eventType,
                                 TargetSystem targetSystem, Object payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(AggregateType.PAYMENT)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .targetSystem(targetSystem)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxRepository.save(event);
            log.debug("Outbox event saved. aggregateId={}, eventType={}, target={}",
                    aggregateId, eventType, targetSystem);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload.", e);
        }
    }

    private PaymentResponse toResponse(org.n11bootcamp.paymentservice.entities.Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .iyzicoPaymentId(payment.getIyzicoPaymentId())
                .failureReason(payment.getFailureReason())
                .cardHolderName(payment.getCardHolderName())
                .cardLastFour(payment.getCardLastFour())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
