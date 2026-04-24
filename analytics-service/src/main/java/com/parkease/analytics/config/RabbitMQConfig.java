package com.parkease.analytics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_EXCHANGE   = "parkease.booking.exchange";
    public static final String ANALYTICS_QUEUE    = "parkease.analytics.queue";

    @Bean
    @Lazy(false)
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    @Lazy(false)
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    @Lazy(false)
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    @Lazy(false)
    public Binding bookingEventsBinding(Queue analyticsQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(analyticsQueue).to(bookingExchange).with("booking.*");
    }

    @Bean
    @Lazy(false)
    public Binding paymentEventsBinding(Queue analyticsQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(analyticsQueue).to(bookingExchange).with("payment.*");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
