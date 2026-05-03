package org.n11bootcamp.paymentservice.services;

import com.iyzipay.Options;
import com.iyzipay.model.Payment;
import com.iyzipay.model.PaymentCard;
import com.iyzipay.request.CreatePaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.paymentservice.dtos.events.PaymentRequestedEvent;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IyzicoPaymentServiceTest {

    @Mock private Options iyzicoOptions;

    @InjectMocks private IyzicoPaymentService iyzicoPaymentService;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private PaymentRequestedEvent sampleEvent() {
        return PaymentRequestedEvent.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .totalAmount(new BigDecimal("100.00"))
                .cardHolderName("Test User")
                .cardNumber("5528790000000008")
                .expireMonth("12")
                .expireYear("2030")
                .cvc("123")
                .shippingAddress("Istanbul, Turkey")
                .build();
    }

    // --- Charge Tests ---

    @Test
    @DisplayName("charge should return payment response from iyzico")
    void charge_success() {
        Payment expectedPayment = mock(Payment.class);
        when(expectedPayment.getStatus()).thenReturn("success");

        try (MockedStatic<Payment> mockedPayment = mockStatic(Payment.class)) {
            mockedPayment.when(() -> Payment.create(any(CreatePaymentRequest.class), eq(iyzicoOptions)))
                    .thenReturn(expectedPayment);

            Payment result = iyzicoPaymentService.charge(sampleEvent());

            assertThat(result).isSameAs(expectedPayment);
            mockedPayment.verify(() -> Payment.create(any(CreatePaymentRequest.class), eq(iyzicoOptions)));
        }
    }

    @Test
    @DisplayName("charge should still return payment when iyzico returns failure")
    void charge_returnsPayment_evenOnFailure() {
        Payment failedPayment = mock(Payment.class);
        when(failedPayment.getStatus()).thenReturn("failure");
        when(failedPayment.getErrorCode()).thenReturn("10084");

        try (MockedStatic<Payment> mockedPayment = mockStatic(Payment.class)) {
            mockedPayment.when(() -> Payment.create(any(), any())).thenReturn(failedPayment);

            Payment result = iyzicoPaymentService.charge(sampleEvent());

            assertThat(result).isSameAs(failedPayment);
        }
    }

    @Test
    @DisplayName("charge should build request with event data and TRY currency")
    void charge_buildsRequestCorrectly() {
        // given
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("success");
        PaymentRequestedEvent event = sampleEvent();

        try (MockedStatic<Payment> mockedPayment = mockStatic(Payment.class)) {
            mockedPayment.when(() -> Payment.create(any(CreatePaymentRequest.class), any(Options.class)))
                    .thenReturn(payment);

            // when
            iyzicoPaymentService.charge(event);

            // then
            ArgumentCaptor<CreatePaymentRequest> captor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
            mockedPayment.verify(() -> Payment.create(captor.capture(), any(Options.class)));

            CreatePaymentRequest captured = captor.getValue();
            assertThat(captured.getConversationId()).isEqualTo(ORDER_ID.toString());
            assertThat(captured.getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(captured.getPaidPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(captured.getCurrency()).isEqualTo("TRY");
            assertThat(captured.getInstallment()).isEqualTo(1);
            assertThat(captured.getBuyer().getId()).isEqualTo(USER_ID.toString());
            assertThat(captured.getShippingAddress().getAddress()).isEqualTo("Istanbul, Turkey");
            assertThat(captured.getBillingAddress().getAddress()).isEqualTo("Istanbul, Turkey");
            assertThat(captured.getBasketItems()).hasSize(1);
        }
    }

    @Test
    @DisplayName("charge should map card details into payment card")
    void charge_mapsCardDetails() {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("success");

        try (MockedStatic<Payment> mockedPayment = mockStatic(Payment.class)) {
            mockedPayment.when(() -> Payment.create(any(), any())).thenReturn(payment);

            iyzicoPaymentService.charge(sampleEvent());

            ArgumentCaptor<CreatePaymentRequest> captor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
            mockedPayment.verify(() -> Payment.create(captor.capture(), any()));

            PaymentCard card = captor.getValue().getPaymentCard();
            assertThat(card.getCardHolderName()).isEqualTo("Test User");
            assertThat(card.getCardNumber()).isEqualTo("5528790000000008");
            assertThat(card.getExpireMonth()).isEqualTo("12");
            assertThat(card.getExpireYear()).isEqualTo("2030");
            assertThat(card.getCvc()).isEqualTo("123");
            assertThat(card.getRegisterCard()).isZero();
        }
    }

    // --- isSuccess Tests ---

    @Test
    @DisplayName("isSuccess should return true when status is success")
    void isSuccess_success() {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("success");

        assertThat(iyzicoPaymentService.isSuccess(payment)).isTrue();
    }

    @Test
    @DisplayName("isSuccess should be case insensitive")
    void isSuccess_caseInsensitive() {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("SUCCESS");

        assertThat(iyzicoPaymentService.isSuccess(payment)).isTrue();
    }

    @Test
    @DisplayName("isSuccess should return false when status is failure")
    void isSuccess_failure() {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("failure");

        assertThat(iyzicoPaymentService.isSuccess(payment)).isFalse();
    }

    @Test
    @DisplayName("isSuccess should return false when status is null")
    void isSuccess_nullStatus() {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn(null);

        assertThat(iyzicoPaymentService.isSuccess(payment)).isFalse();
    }

    // --- extractFailureReason Tests ---

    @Test
    @DisplayName("extractFailureReason should return errorMessage when present")
    void extractFailureReason_returnsErrorMessage() {
        Payment payment = mock(Payment.class);
        when(payment.getErrorMessage()).thenReturn("Card was declined");

        assertThat(iyzicoPaymentService.extractFailureReason(payment))
                .isEqualTo("Card was declined");
    }

    @Test
    @DisplayName("extractFailureReason should return error code message when message is null")
    void extractFailureReason_returnsErrorCode_whenMessageNull() {
        Payment payment = mock(Payment.class);
        when(payment.getErrorCode()).thenReturn("10084");

        assertThat(iyzicoPaymentService.extractFailureReason(payment))
                .isEqualTo("Error code: 10084");
    }

    @Test
    @DisplayName("extractFailureReason should return default message when both are null")
    void extractFailureReason_returnsDefault_whenAllNull() {
        Payment payment = mock(Payment.class);

        assertThat(iyzicoPaymentService.extractFailureReason(payment))
                .isEqualTo("Payment failed");
    }

    @Test
    @DisplayName("extractFailureReason should prefer errorMessage over errorCode")
    void extractFailureReason_prefersMessage_overCode() {
        Payment payment = mock(Payment.class);
        when(payment.getErrorMessage()).thenReturn("Insufficient funds");

        assertThat(iyzicoPaymentService.extractFailureReason(payment))
                .isEqualTo("Insufficient funds");
    }

    // --- extractLastFour Tests ---

    @Test
    @DisplayName("extractLastFour should return last 4 digits of card number")
    void extractLastFour_validCard() {
        assertThat(iyzicoPaymentService.extractLastFour("5528790000000008"))
                .isEqualTo("0008");
    }

    @Test
    @DisplayName("extractLastFour should return 0000 when card number is null")
    void extractLastFour_nullCard() {
        assertThat(iyzicoPaymentService.extractLastFour(null)).isEqualTo("0000");
    }

    @Test
    @DisplayName("extractLastFour should return 0000 when card number is shorter than 4")
    void extractLastFour_tooShort() {
        assertThat(iyzicoPaymentService.extractLastFour("12")).isEqualTo("0000");
    }

    @Test
    @DisplayName("extractLastFour should return entire string when length is exactly 4")
    void extractLastFour_exactly4() {
        assertThat(iyzicoPaymentService.extractLastFour("1234")).isEqualTo("1234");
    }

    @Test
    @DisplayName("extractLastFour should return 0000 when card number is empty")
    void extractLastFour_empty() {
        assertThat(iyzicoPaymentService.extractLastFour("")).isEqualTo("0000");
    }
}