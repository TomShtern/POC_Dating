package com.dating.recommendation.config;

import com.dating.common.config.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event consumption.
 * Defines queues and bindings for user events that trigger recommendation updates.
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Topic exchange for user events (already defined by user-service).
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(RabbitMQConstants.USER_EXCHANGE);
    }

    /**
     * Queue for recommendation service to consume user registration events.
     */
    @Bean
    public Queue recommendationUserRegisteredQueue() {
        return QueueBuilder.durable(RabbitMQConstants.RECOMMENDATION_USER_REGISTERED_QUEUE)
                .build();
    }

    /**
     * Queue for recommendation service to consume user update events.
     */
    @Bean
    public Queue recommendationUserUpdatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.RECOMMENDATION_USER_UPDATED_QUEUE)
                .build();
    }

    /**
     * Binding for user registered events.
     */
    @Bean
    public Binding recommendationUserRegisteredBinding(
            Queue recommendationUserRegisteredQueue,
            TopicExchange userExchange) {
        return BindingBuilder.bind(recommendationUserRegisteredQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_REGISTERED_KEY);
    }

    /**
     * Binding for user updated events.
     */
    @Bean
    public Binding recommendationUserUpdatedBinding(
            Queue recommendationUserUpdatedQueue,
            TopicExchange userExchange) {
        return BindingBuilder.bind(recommendationUserUpdatedQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_UPDATED_KEY);
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
