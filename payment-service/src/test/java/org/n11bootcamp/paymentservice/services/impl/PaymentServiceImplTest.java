package org.n11bootcamp.paymentservice.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyzipay.model.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.paymentservice.dtos.events.PaymentRequestedEvent;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private IyzicoPaymentService iyzicoPaymentService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private PaymentServiceImpl paymentService;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    private PaymentRequestedEvent sampleEvent() {
        return PaymentRequestedEvent.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .totalAmount(AMOUNT)
                .cardHolderName("Test User")
                .cardNumber("5528790000000008")
                .expireMonth("12")
                .expireYear("2030")
                .cvc("123")
                .shippingAddress("Istanbul, Turkey")
                .build();
    }

    private org.mockito.stubbing.Answer<org.n11bootcamp.paymentservice.entities.Payment> assignIdOnSave() {
        return inv -> {
            org.n11bootcamp.paymentservice.entities.Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(PAYMENT_ID);
            }
            return p;
        };
    }

    // --- processPayment Tests ---

    @Test
    @DisplayName("processPayment should ignore duplicate events (Idempotency)")
    void processPayment_idempotent() {
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        paymentService.processPayment(sampleEvent());

        verify(paymentRepository, never()).save(any());
        verify(iyzicoPaymentService, never()).charge(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment should complete payment when iyzico returns success")
    void processPayment_success() throws JsonProcessingException {
        // given
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(iyzicoPaymentService.extractLastFour(anyString())).thenReturn("0008");
        when(paymentRepository.save(any(org.n11bootcamp.paymentservice.entities.Payment.class)))
                .thenAnswer(assignIdOnSave());

        Payment iyzicoResponse = mock(Payment.class);
        when(iyzicoResponse.getPaymentId()).thenReturn("IYZ-PAY-123");
        when(iyzicoPaymentService.charge(any())).thenReturn(iyzicoResponse);
        when(iyzicoPaymentService.isSuccess(iyzicoResponse)).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        paymentService.processPayment(sampleEvent());

        // then
        verify(paymentRepository, times(2))
                .save(any(org.n11bootcamp.paymentservice.entities.Payment.class));

        ArgumentCaptor<org.n11bootcamp.paymentservice.entities.Payment> paymentCaptor =
                ArgumentCaptor.forClass(org.n11bootcamp.paymentservice.entities.Payment.class);
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());

        org.n11bootcamp.paymentservice.entities.Payment finalPayment =
                paymentCaptor.getAllValues().get(1);
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(finalPayment.getIyzicoPaymentId()).isEqualTo("IYZ-PAY-123");
        assertThat(finalPayment.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(finalPayment.getUserId()).isEqualTo(USER_ID);
        assertThat(finalPayment.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(finalPayment.getCardHolderName()).isEqualTo("Test User");
        assertThat(finalPayment.getCardLastFour()).isEqualTo("0008");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxEvent outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.getEventType()).isEqualTo(EventType.PAYMENT_COMPLETED);
        assertThat(outboxEvent.getAggregateType()).isEqualTo(AggregateType.PAYMENT);
        assertThat(outboxEvent.getTargetSystem()).isEqualTo(TargetSystem.ORDER_SERVICE);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(PAYMENT_ID.toString());
    }

    @Test
    @DisplayName("processPayment should mark as FAILED when iyzico returns failure")
    void processPayment_iyzicoReturnsFailure() throws JsonProcessingException {
        // given
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(iyzicoPaymentService.extractLastFour(anyString())).thenReturn("0008");
        when(paymentRepository.save(any(org.n11bootcamp.paymentservice.entities.Payment.class)))
                .thenAnswer(assignIdOnSave());

        Payment iyzicoResponse = mock(Payment.class);
        when(iyzicoPaymentService.charge(any())).thenReturn(iyzicoResponse);
        when(iyzicoPaymentService.isSuccess(iyzicoResponse)).thenReturn(false);
        when(iyzicoPaymentService.extractFailureReason(iyzicoResponse)).thenReturn("Card declined");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        paymentService.processPayment(sampleEvent());

        // then
        ArgumentCaptor<org.n11bootcamp.paymentservice.entities.Payment> paymentCaptor =
                ArgumentCaptor.forClass(org.n11bootcamp.paymentservice.entities.Payment.class);
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());

        org.n11bootcamp.paymentservice.entities.Payment finalPayment =
                paymentCaptor.getAllValues().get(1);
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(finalPayment.getFailureReason()).isEqualTo("Card declined");
        assertThat(finalPayment.getIyzicoPaymentId()).isNull();

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("processPayment should mark as FAILED when iyzico throws exception")
    void processPayment_iyzicoThrowsException() throws JsonProcessingException {
        // given
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(iyzicoPaymentService.extractLastFour(anyString())).thenReturn("0008");
        when(paymentRepository.save(any(org.n11bootcamp.paymentservice.entities.Payment.class)))
                .thenAnswer(assignIdOnSave());
        when(iyzicoPaymentService.charge(any()))
                .thenThrow(new RuntimeException("Connection timeout"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // when
        paymentService.processPayment(sampleEvent());

        // then
        ArgumentCaptor<org.n11bootcamp.paymentservice.entities.Payment> paymentCaptor =
                ArgumentCaptor.forClass(org.n11bootcamp.paymentservice.entities.Payment.class);
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());

        org.n11bootcamp.paymentservice.entities.Payment finalPayment =
                paymentCaptor.getAllValues().get(1);
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(finalPayment.getFailureReason())
                .startsWith("iyzico connection error:")
                .contains("Connection timeout");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.PAYMENT_FAILED);

        verify(iyzicoPaymentService, never()).isSuccess(any());
    }

    @Test
    @DisplayName("processPayment should save initial payment with PENDING status before charging iyzico")
    void processPayment_savesPendingFirst() throws JsonProcessingException {
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(iyzicoPaymentService.extractLastFour(anyString())).thenReturn("0008");
        when(paymentRepository.save(any(org.n11bootcamp.paymentservice.entities.Payment.class)))
                .thenAnswer(assignIdOnSave());
        when(iyzicoPaymentService.charge(any())).thenReturn(mock(Payment.class));
        when(iyzicoPaymentService.isSuccess(any())).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        paymentService.processPayment(sampleEvent());

        InOrder inOrder = inOrder(paymentRepository, iyzicoPaymentService);
        inOrder.verify(paymentRepository).save(any(org.n11bootcamp.paymentservice.entities.Payment.class));
        inOrder.verify(iyzicoPaymentService).charge(any());
        inOrder.verify(paymentRepository).save(any(org.n11bootcamp.paymentservice.entities.Payment.class));
    }

    // --- getByOrderId Tests ---

    @Test
    @DisplayName("getByOrderId should return payment response when found")
    void getByOrderId_success() {
        org.n11bootcamp.paymentservice.entities.Payment payment =
                org.n11bootcamp.paymentservice.entities.Payment.builder()
                        .id(PAYMENT_ID)
                        .orderId(ORDER_ID)
                        .userId(USER_ID)
                        .amount(AMOUNT)
                        .status(PaymentStatus.COMPLETED)
                        .iyzicoPaymentId("IYZ-PAY-123")
                        .cardHolderName("Test User")
                        .cardLastFour("0008")
                        .build();

        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getByOrderId(ORDER_ID);

        assertThat(response.getId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getIyzicoPaymentId()).isEqualTo("IYZ-PAY-123");
        assertThat(response.getCardHolderName()).isEqualTo("Test User");
        assertThat(response.getCardLastFour()).isEqualTo("0008");
    }

    @Test
    @DisplayName("getByOrderId should throw exception when payment not found")
    void getByOrderId_notFound() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByOrderId(ORDER_ID))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining(ORDER_ID.toString());
    }
}