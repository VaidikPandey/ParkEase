package com.parkease.booking.config;

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

    // ── Exchange, messages go into exchange first (it is a router)
    public static final String BOOKING_EXCHANGE = "parkease.booking.exchange";

    // ── Routing Keys
    public static final String BOOKING_CONFIRMED_KEY  = "booking.confirmed";
    public static final String BOOKING_CANCELLED_KEY  = "booking.cancelled";
    public static final String BOOKING_CHECKIN_KEY    = "booking.checkin";
    public static final String BOOKING_CHECKOUT_KEY   = "booking.checkout";
    public static final String BOOKING_EXPIRY_KEY     = "booking.expiry";
    public static final String BOOKING_REMINDER_KEY   = "booking.reminder";

    // ── Queues, a mailbox. notification-service listens to this queue
    public static final String NOTIFICATION_QUEUE = "parkease.notification.queue";

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
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    @Lazy(false)
    public Binding notificationBinding(Queue notificationQueue, TopicExchange bookingExchange) { // connects exchange to queue.
        // booking.* means any routing key starting with booking. gets routed to the notification queue
        return BindingBuilder
                .bind(notificationQueue)
                .to(bookingExchange)
                .with("booking.*");
    }

    @Bean
    public MessageConverter messageConverter() { // converts Java objects to JSON automatically before sending to RabbitMQ
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}