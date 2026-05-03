package org.n11bootcamp.inventoryservice.configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${messaging.exchanges.inventory}")
    private String inventoryExchange;

    @Value("${messaging.exchanges.product}")
    private String productExchange;

    @Value("${messaging.exchanges.order}")
    private String orderExchange;

    @Value("${messaging.queues.product-created}")
    private String productCreatedQueue;

    @Value("${messaging.queues.product-deleted}")
    private String productDeletedQueue;

    @Value("${messaging.queues.order-created}")
    private String orderCreatedQueue;

    @Value("${messaging.queues.order-cancelled}")
    private String orderCancelledQueue;

    @Value("${messaging.routing-keys.product-created}")
    private String productCreatedRoutingKey;

    @Value("${messaging.routing-keys.product-deleted}")
    private String productDeletedRoutingKey;

    @Value("${messaging.routing-keys.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${messaging.routing-keys.order-cancelled}")
    private String orderCancelledRoutingKey;

    @Value("${messaging.routing-keys.stock-released}")
    private String stockReleasedRoutingKey;

    @Value("${messaging.queues.order-confirmed}")
    private String orderConfirmedQueue;

    @Value("${messaging.routing-keys.order-confirmed}")
    private String orderConfirmedRoutingKey;

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(orderConfirmedQueue).build();
    }

    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder
                .bind(orderConfirmedQueue())
                .to(orderExchange())
                .with(orderConfirmedRoutingKey);
    }

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(inventoryExchange, true, false);
    }

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(productExchange, true, false);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue productCreatedQueue() {
        return QueueBuilder.durable(productCreatedQueue).build();
    }

    @Bean
    public Queue productDeletedQueue() {
        return QueueBuilder.durable(productDeletedQueue).build();
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueue).build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(orderCancelledQueue).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding productCreatedBinding() {
        return BindingBuilder
                .bind(productCreatedQueue())
                .to(productExchange())
                .with(productCreatedRoutingKey);
    }

    @Bean
    public Binding orderCancelledByStockReleasedBinding() {
        return BindingBuilder
                .bind(orderCancelledQueue())
                .to(orderExchange())
                .with(stockReleasedRoutingKey);  // stock.released
    }

    @Bean
    public Binding productDeletedBinding() {
        return BindingBuilder
                .bind(productDeletedQueue())
                .to(productExchange())
                .with(productDeletedRoutingKey);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(orderExchange())        //from productExchange to orderExchange
                .with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder
                .bind(orderCancelledQueue())
                .to(orderExchange())        //from productExchange to orderExchange
                .with(orderCancelledRoutingKey);
    }

    // ── Converter & Template ──────────────────────────────────────────────────

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