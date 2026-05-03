package org.n11bootcamp.paymentservice.services;



import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import com.iyzipay.Options;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.n11bootcamp.paymentservice.dtos.events.*;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IyzicoPaymentService {

    private final Options iyzicoOptions;

    public Payment charge(PaymentRequestedEvent event) {
        CreatePaymentRequest request = buildPaymentRequest(event);

        log.debug("Sending payment request to iyzico. orderId={}", event.getOrderId());

        Payment payment = Payment.create(request, iyzicoOptions);

        log.info("iyzico response received. orderId={}, status={}, errorCode={}",
                event.getOrderId(), payment.getStatus(), payment.getErrorCode());

        return payment;
    }

    private CreatePaymentRequest buildPaymentRequest(PaymentRequestedEvent event) {
        CreatePaymentRequest request = new CreatePaymentRequest();

        request.setLocale(Locale.TR.getValue());
        request.setConversationId(event.getOrderId().toString());
        request.setPrice(event.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        request.setPaidPrice(event.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        request.setCurrency(Currency.TRY.name());
        request.setInstallment(1);
        request.setPaymentChannel(PaymentChannel.WEB.name());
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());

        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(event.getCardHolderName());
        paymentCard.setCardNumber(event.getCardNumber());
        paymentCard.setExpireMonth(event.getExpireMonth());
        paymentCard.setExpireYear(event.getExpireYear());
        paymentCard.setCvc(event.getCvc());
        paymentCard.setRegisterCard(0);
        request.setPaymentCard(paymentCard);

        Buyer buyer = new Buyer();
        buyer.setId(event.getUserId().toString());
        buyer.setName("Test");
        buyer.setSurname("User");
        buyer.setEmail("test@test.com");
        buyer.setIdentityNumber("74300864791");
        buyer.setRegistrationAddress(event.getShippingAddress());
        buyer.setIp("85.34.78.112");
        buyer.setCity("Istanbul");
        buyer.setCountry("Turkey");
        request.setBuyer(buyer);
        buyer.setRegistrationAddress(event.getShippingAddress());

        Address shippingAddress = new Address();
        shippingAddress.setContactName(event.getCardHolderName());
        shippingAddress.setCity("Istanbul");
        shippingAddress.setCountry("Turkey");
        shippingAddress.setAddress(event.getShippingAddress());
        request.setShippingAddress(shippingAddress);

        Address billingAddress = new Address();
        billingAddress.setContactName(event.getCardHolderName());
        billingAddress.setCity("Istanbul");
        billingAddress.setCountry("Turkey");
        billingAddress.setAddress(event.getShippingAddress());
        request.setBillingAddress(billingAddress);

        List<BasketItem> basketItems = new ArrayList<>();
        BasketItem basketItem = new BasketItem();
        basketItem.setId(UUID.randomUUID().toString());
        basketItem.setName("Order Items");
        basketItem.setCategory1("General");
        basketItem.setItemType(BasketItemType.PHYSICAL.name());
        basketItem.setPrice(event.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        basketItems.add(basketItem);
        request.setBasketItems(basketItems);

        return request;
    }

    public boolean isSuccess(Payment payment) {
        return "success".equalsIgnoreCase(payment.getStatus());
    }

    public String extractFailureReason(Payment payment) {
        if (payment.getErrorMessage() != null) {
            return payment.getErrorMessage();
        }
        if (payment.getErrorCode() != null) {
            return "Error code: " + payment.getErrorCode();
        }
        return "Payment failed";
    }

    public String extractLastFour(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "0000";
        return cardNumber.substring(cardNumber.length() - 4);
    }
}
