package org.n11bootcamp.paymentservice.configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${messaging.exchanges.payment}")
    private String paymentExchange;

    @Value("${messaging.exchanges.order}")
    private String orderExchange;

    @Value("${messaging.queues.payment-requested}")
    private String paymentRequestedQueue;

    @Value("${messaging.routing-keys.payment-requested}")
    private String paymentRequestedRoutingKey;



    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(paymentExchange, true, false);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }



    @Bean
    public TopicExchange paymentDlx() {
        return new TopicExchange("payment.dlx", true, false);
    }



    @Bean
    public Queue paymentRequestedQueue() {
        return QueueBuilder.durable(paymentRequestedQueue)
                .withArgument("x-dead-letter-exchange", "payment.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.payment.requested")
                .build();
    }



    @Bean
    public Queue dlqPaymentRequested() {
        return QueueBuilder.durable("dlq.payment.requested").build();
    }



    @Bean
    public Binding paymentRequestedBinding() {
        return BindingBuilder
                .bind(paymentRequestedQueue())
                .to(orderExchange())
                .with(paymentRequestedRoutingKey);
    }

    @Bean
    public Binding dlqPaymentRequestedBinding() {
        return BindingBuilder
                .bind(dlqPaymentRequested())
                .to(paymentDlx())
                .with("dlq.payment.requested");
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
