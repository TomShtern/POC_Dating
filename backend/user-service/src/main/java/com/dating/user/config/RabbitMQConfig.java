package com.dating.user.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event publishing.
 * Defines exchanges, queues, and bindings for user events.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String USER_EXCHANGE = "user.exchange";

    // Routing keys
    public static final String USER_REGISTERED_KEY = "user.registered";
    public static final String USER_UPDATED_KEY = "user.updated";
    public static final String USER_DELETED_KEY = "user.deleted";

    /**
     * Topic exchange for user events.
     * Allows routing based on routing key patterns.
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE);
    }

    /**
     * Queue for user registered events.
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable("user.registered.queue")
                .build();
    }

    /**
     * Queue for user updated events.
     */
    @Bean
    public Queue userUpdatedQueue() {
        return QueueBuilder.durable("user.updated.queue")
                .build();
    }

    /**
     * Queue for user deleted events.
     */
    @Bean
    public Queue userDeletedQueue() {
        return QueueBuilder.durable("user.deleted.queue")
                .build();
    }

    /**
     * Binding for user registered events.
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(userExchange)
                .with(USER_REGISTERED_KEY);
    }

    /**
     * Binding for user updated events.
     */
    @Bean
    public Binding userUpdatedBinding(Queue userUpdatedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userUpdatedQueue)
                .to(userExchange)
                .with(USER_UPDATED_KEY);
    }

    /**
     * Binding for user deleted events.
     */
    @Bean
    public Binding userDeletedBinding(Queue userDeletedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueue)
                .to(userExchange)
                .with(USER_DELETED_KEY);
    }

    /**
     * JSON message converter for RabbitMQ messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitMQ template configured with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
