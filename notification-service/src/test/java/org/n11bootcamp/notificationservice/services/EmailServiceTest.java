package org.n11bootcamp.notificationservice.services;

import static org.junit.jupiter.api.Assertions.*;



import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.notificationservice.dtos.events.OrderConfirmedEvent;
import org.n11bootcamp.notificationservice.dtos.events.OrderConfirmedEventItem;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    private final String FROM_EMAIL = "no-reply@n11bootcamp.com";

    @BeforeEach
    void setUp() {
        // @Value("${notification.mail.from}") alanını setliyoruz
        ReflectionTestUtils.setField(emailService, "from", FROM_EMAIL);
    }

    private OrderConfirmedEvent createSampleEvent() {
        OrderConfirmedEventItem item = new OrderConfirmedEventItem();
        item.setProductName("MacBook Air M3");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("50000"));
        item.setTotalPrice(new BigDecimal("50000"));

        OrderConfirmedEvent event = new OrderConfirmedEvent();
        event.setOrderId(UUID.randomUUID());
        event.setBuyerEmail("buyer@gmail.com");
        event.setSellerEmail("seller@n11.com");
        event.setItems(List.of(item));
        event.setTotalAmount(new BigDecimal("50000"));
        return event;
    }

    @Test
    @DisplayName("sendOrderConfirmedToBuyer: should build content and send email to buyer")
    void sendOrderConfirmedToBuyer_success() {
        // given
        OrderConfirmedEvent event = createSampleEvent();
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendOrderConfirmedToBuyer(event);

        // then
        verify(mailSender).send(mimeMessage);
        verify(mailSender).createMimeMessage();
    }

    @Test
    @DisplayName("sendOrderConfirmedToSeller: should build content and send email to seller")
    void sendOrderConfirmedToSeller_success() {
        // given
        OrderConfirmedEvent event = createSampleEvent();
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendOrderConfirmedToSeller(event);

        // then
        verify(mailSender).send(mimeMessage);
        verify(mailSender).createMimeMessage();
    }

    @Test
    @DisplayName("sendEmail: should handle exception and log error when mailSender fails")
    void sendEmail_shouldHandleException() {
        // given
        OrderConfirmedEvent event = createSampleEvent();
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Mail gönderimi sırasında hata fırlatıldığını simüle edelim
        doThrow(new RuntimeException("SMTP Connection Failed")).when(mailSender).send(any(MimeMessage.class));

        // when
        // sendEmail private olduğu için public metod üzerinden tetikliyoruz
        emailService.sendOrderConfirmedToBuyer(event);

        // then
        // Uygulama patlamamalı (try-catch sayesinde), sadece log atmalı.
        verify(mailSender).send(mimeMessage);
    }
}