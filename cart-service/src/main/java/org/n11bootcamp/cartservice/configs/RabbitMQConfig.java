package org.n11bootcamp.cartservice.configs;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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

    @Value("${messaging.queues.stock-updated}")
    private String stockUpdatedQueue;

    @Value("${messaging.routing-keys.stock-updated}")
    private String stockUpdatedRoutingKey;

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(inventoryExchange, true, false);
    }

    @Bean
    public Queue stockUpdatedQueue() {
        return QueueBuilder.durable(stockUpdatedQueue).build();
    }

    @Bean
    public Binding stockUpdatedBinding() {
        return BindingBuilder
                .bind(stockUpdatedQueue())
                .to(inventoryExchange())
                .with(stockUpdatedRoutingKey);
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