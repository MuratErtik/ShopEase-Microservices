package org.n11bootcamp.orderservice.configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${messaging.exchanges.order}")
    private String orderExchange;

    @Value("${messaging.exchanges.inventory}")
    private String inventoryExchange;

    @Value("${messaging.exchanges.payment}")
    private String paymentExchange;

    @Value("${messaging.queues.stock-reserved}")
    private String stockReservedQueue;

    @Value("${messaging.queues.stock-reservation-failed}")
    private String stockReservationFailedQueue;

    @Value("${messaging.queues.payment-completed}")
    private String paymentCompletedQueue;

    @Value("${messaging.queues.payment-failed}")
    private String paymentFailedQueue;

    @Value("${messaging.routing-keys.stock-reserved}")
    private String stockReservedRoutingKey;

    @Value("${messaging.routing-keys.stock-reservation-failed}")
    private String stockReservationFailedRoutingKey;

    @Value("${messaging.routing-keys.payment-completed}")
    private String paymentCompletedRoutingKey;

    @Value("${messaging.routing-keys.payment-failed}")
    private String paymentFailedRoutingKey;



    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(inventoryExchange, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(paymentExchange, true, false);
    }



    @Bean
    public TopicExchange orderDlx() {
        return new TopicExchange("order.dlx", true, false);
    }



    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(stockReservedQueue)
                .withArgument("x-dead-letter-exchange", "order.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.stock.reserved")
                .build();
    }

    @Bean
    public Queue stockReservationFailedQueue() {
        return QueueBuilder.durable(stockReservationFailedQueue)
                .withArgument("x-dead-letter-exchange", "order.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.stock.reservation.failed")
                .build();
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(paymentCompletedQueue)
                .withArgument("x-dead-letter-exchange", "order.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.payment.completed")
                .build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueue)
                .withArgument("x-dead-letter-exchange", "order.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.payment.failed")
                .build();
    }



    @Bean
    public Queue dlqStockReserved() {
        return QueueBuilder.durable("dlq.order.stock.reserved").build();
    }

    @Bean
    public Queue dlqStockReservationFailed() {
        return QueueBuilder.durable("dlq.order.stock.reservation.failed").build();
    }

    @Bean
    public Queue dlqPaymentCompleted() {
        return QueueBuilder.durable("dlq.order.payment.completed").build();
    }

    @Bean
    public Queue dlqPaymentFailed() {
        return QueueBuilder.durable("dlq.order.payment.failed").build();
    }



    @Bean
    public Binding stockReservedBinding() {
        return BindingBuilder
                .bind(stockReservedQueue())
                .to(inventoryExchange())
                .with(stockReservedRoutingKey);
    }

    @Bean
    public Binding stockReservationFailedBinding() {
        return BindingBuilder
                .bind(stockReservationFailedQueue())
                .to(inventoryExchange())
                .with(stockReservationFailedRoutingKey);
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder
                .bind(paymentCompletedQueue())
                .to(paymentExchange())
                .with(paymentCompletedRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(paymentExchange())
                .with(paymentFailedRoutingKey);
    }



    @Bean
    public Binding dlqStockReservedBinding() {
        return BindingBuilder
                .bind(dlqStockReserved())
                .to(orderDlx())
                .with("dlq.stock.reserved");
    }

    @Bean
    public Binding dlqStockReservationFailedBinding() {
        return BindingBuilder
                .bind(dlqStockReservationFailed())
                .to(orderDlx())
                .with("dlq.stock.reservation.failed");
    }

    @Bean
    public Binding dlqPaymentCompletedBinding() {
        return BindingBuilder
                .bind(dlqPaymentCompleted())
                .to(orderDlx())
                .with("dlq.payment.completed");
    }

    @Bean
    public Binding dlqPaymentFailedBinding() {
        return BindingBuilder
                .bind(dlqPaymentFailed())
                .to(orderDlx())
                .with("dlq.payment.failed");
    }



    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
