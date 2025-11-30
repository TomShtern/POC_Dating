package com.dating.user.config;

import com.dating.common.config.DeadLetterConfig;
import com.dating.common.config.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RabbitMQ configuration for event publishing.
 * Defines exchanges, queues, and bindings for user events.
 * Imports DeadLetterConfig to ensure dead letter exchange/queue are created.
 */
@Configuration
@Import(DeadLetterConfig.class)
public class RabbitMQConfig {

    /**
     * Topic exchange for user events.
     * Allows routing based on routing key patterns.
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(RabbitMQConstants.USER_EXCHANGE);
    }

    /**
     * Queue for user registered events with dead letter support.
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_REGISTERED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for user updated events with dead letter support.
     */
    @Bean
    public Queue userUpdatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for user deleted events with dead letter support.
     */
    @Bean
    public Queue userDeletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Binding for user registered events.
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_REGISTERED_KEY);
    }

    /**
     * Binding for user updated events.
     */
    @Bean
    public Binding userUpdatedBinding(Queue userUpdatedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userUpdatedQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_UPDATED_KEY);
    }

    /**
     * Binding for user deleted events.
     */
    @Bean
    public Binding userDeletedBinding(Queue userDeletedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_DELETED_KEY);
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
