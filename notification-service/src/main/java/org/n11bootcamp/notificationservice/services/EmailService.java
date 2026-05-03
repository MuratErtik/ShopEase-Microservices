package org.n11bootcamp.notificationservice.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.n11bootcamp.notificationservice.dtos.events.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from}")
    private String from;

    public void sendOrderConfirmedToBuyer(OrderConfirmedEvent event) {
        String subject = "Your order has been confirmed! #" + event.getOrderId();
        String content = buildBuyerEmail(event);
        sendEmail(event.getBuyerEmail(), subject, content);
        log.info("Order confirmed email sent to buyer. orderId={}, email={}",
                event.getOrderId(), event.getBuyerEmail());
    }

    public void sendOrderConfirmedToSeller(OrderConfirmedEvent event) {
        String subject = "You have a new confirmed order! #" + event.getOrderId();
        String content = buildSellerEmail(event);
        sendEmail(event.getSellerEmail(), subject, content);
        log.info("Order confirmed email sent to seller. orderId={}, email={}",
                event.getOrderId(), event.getSellerEmail());
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email. to={}, subject={}, error={}",
                    to, subject, e.getMessage());
        }
    }

    private String buildBuyerEmail(OrderConfirmedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Your order has been confirmed!</h2>");
        sb.append("<p>Order ID: <strong>").append(event.getOrderId()).append("</strong></p>");
        sb.append("<h3>Order Items:</h3>");
        sb.append("<table border='1' cellpadding='8'>");
        sb.append("<tr><th>Product</th><th>Quantity</th><th>Unit Price</th><th>Total</th></tr>");

        for (OrderConfirmedEventItem item : event.getItems()) {
            sb.append("<tr>");
            sb.append("<td>").append(item.getProductName()).append("</td>");
            sb.append("<td>").append(item.getQuantity()).append("</td>");
            sb.append("<td>").append(item.getUnitPrice()).append(" TRY</td>");
            sb.append("<td>").append(item.getTotalPrice()).append(" TRY</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("<p><strong>Total Amount: ")
                .append(event.getTotalAmount())
                .append(" TRY</strong></p>");
        sb.append("<p>Thank you for your purchase!</p>");
        return sb.toString();
    }

    private String buildSellerEmail(OrderConfirmedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>You have a new confirmed order!</h2>");
        sb.append("<p>Order ID: <strong>").append(event.getOrderId()).append("</strong></p>");
        sb.append("<h3>Order Items:</h3>");
        sb.append("<table border='1' cellpadding='8'>");
        sb.append("<tr><th>Product</th><th>Quantity</th><th>Unit Price</th><th>Total</th></tr>");

        for (OrderConfirmedEventItem item : event.getItems()) {
            sb.append("<tr>");
            sb.append("<td>").append(item.getProductName()).append("</td>");
            sb.append("<td>").append(item.getQuantity()).append("</td>");
            sb.append("<td>").append(item.getUnitPrice()).append(" TRY</td>");
            sb.append("<td>").append(item.getTotalPrice()).append(" TRY</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("<p><strong>Total Amount: ")
                .append(event.getTotalAmount())
                .append(" TRY</strong></p>");
        return sb.toString();
    }
}
