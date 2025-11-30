package com.dating.match.config;

import com.dating.common.config.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Match Service.
 * Defines exchanges, queues, and bindings for match events
 * and consuming user events.
 */
@Configuration
public class RabbitMQConfig {

    // ===========================================
    // EXCHANGES
    // ===========================================

    /**
     * Topic exchange for match-related events.
     */
    @Bean
    TopicExchange matchExchange() {
        return new TopicExchange(RabbitMQConstants.MATCH_EXCHANGE);
    }

    /**
     * Topic exchange for user events (to bind to).
     */
    @Bean
    TopicExchange userExchange() {
        return new TopicExchange(RabbitMQConstants.USER_EXCHANGE);
    }

    // ===========================================
    // QUEUES - PUBLISHING
    // ===========================================

    /**
     * Queue for match created events with dead letter support.
     */
    @Bean
    Queue matchCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MATCH_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for match ended events with dead letter support.
     */
    @Bean
    Queue matchEndedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MATCH_ENDED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    // ===========================================
    // QUEUES - CONSUMING
    // ===========================================

    /**
     * Queue for consuming user registered events with dead letter support.
     */
    @Bean
    Queue matchUserRegisteredQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MATCH_USER_REGISTERED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for consuming user updated events with dead letter support.
     */
    @Bean
    Queue matchUserUpdatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MATCH_USER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for consuming user deleted events with dead letter support.
     */
    @Bean
    Queue matchUserDeletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MATCH_USER_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    // ===========================================
    // BINDINGS - PUBLISHING
    // ===========================================

    /**
     * Binding for match created events.
     */
    @Bean
    Binding matchCreatedBinding(Queue matchCreatedQueue, TopicExchange matchExchange) {
        return BindingBuilder.bind(matchCreatedQueue)
                .to(matchExchange)
                .with(RabbitMQConstants.MATCH_CREATED_KEY);
    }

    /**
     * Binding for match ended events.
     */
    @Bean
    Binding matchEndedBinding(Queue matchEndedQueue, TopicExchange matchExchange) {
        return BindingBuilder.bind(matchEndedQueue)
                .to(matchExchange)
                .with(RabbitMQConstants.MATCH_ENDED_KEY);
    }

    // ===========================================
    // BINDINGS - CONSUMING
    // ===========================================

    /**
     * Binding for user registered events.
     */
    @Bean
    Binding userRegisteredBinding(Queue matchUserRegisteredQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(matchUserRegisteredQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_REGISTERED_KEY);
    }

    /**
     * Binding for user updated events.
     */
    @Bean
    Binding userUpdatedBinding(Queue matchUserUpdatedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(matchUserUpdatedQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_UPDATED_KEY);
    }

    /**
     * Binding for user deleted events.
     */
    @Bean
    Binding userDeletedBinding(Queue matchUserDeletedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(matchUserDeletedQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_DELETED_KEY);
    }

    // ===========================================
    // MESSAGE CONVERTER
    // ===========================================

    /**
     * JSON message converter for RabbitMQ messages.
     */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitMQ template configured with JSON converter.
     */
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
