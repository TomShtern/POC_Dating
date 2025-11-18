package com.dating.chat.config;

import com.dating.common.config.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event publishing and consuming.
 * Chat service publishes message events and consumes match events.
 */
@Configuration
public class RabbitMQConfig {

    // ========================================
    // EXCHANGES
    // ========================================

    /**
     * Topic exchange for chat/message events.
     */
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(RabbitMQConstants.CHAT_EXCHANGE);
    }

    /**
     * Topic exchange for match events (for consuming).
     */
    @Bean
    public TopicExchange matchExchange() {
        return new TopicExchange(RabbitMQConstants.MATCH_EXCHANGE);
    }

    /**
     * Topic exchange for user events (for consuming).
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(RabbitMQConstants.USER_EXCHANGE);
    }

    // ========================================
    // QUEUES - PUBLISHING
    // ========================================

    /**
     * Queue for message sent events with dead letter support.
     */
    @Bean
    public Queue messageSentQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MESSAGE_SENT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for message read events with dead letter support.
     */
    @Bean
    public Queue messageReadQueue() {
        return QueueBuilder.durable(RabbitMQConstants.MESSAGE_READ_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    // ========================================
    // QUEUES - CONSUMING
    // ========================================

    /**
     * Queue for chat service to consume match creation events with dead letter support.
     */
    @Bean
    public Queue chatMatchCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.CHAT_MATCH_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for chat service to consume match ended events with dead letter support.
     */
    @Bean
    public Queue chatMatchEndedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.CHAT_MATCH_ENDED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    /**
     * Queue for chat service to consume user deletion events with dead letter support.
     */
    @Bean
    public Queue chatUserDeletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.CHAT_USER_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY)
                .build();
    }

    // ========================================
    // BINDINGS - PUBLISHING
    // ========================================

    /**
     * Binding for message sent events.
     */
    @Bean
    public Binding messageSentBinding(Queue messageSentQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(messageSentQueue)
                .to(chatExchange)
                .with(RabbitMQConstants.MESSAGE_SENT_KEY);
    }

    /**
     * Binding for message read events.
     */
    @Bean
    public Binding messageReadBinding(Queue messageReadQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(messageReadQueue)
                .to(chatExchange)
                .with(RabbitMQConstants.MESSAGE_READ_KEY);
    }

    // ========================================
    // BINDINGS - CONSUMING
    // ========================================

    /**
     * Binding for chat service to receive match creation events.
     */
    @Bean
    public Binding chatMatchCreatedBinding(Queue chatMatchCreatedQueue, TopicExchange matchExchange) {
        return BindingBuilder.bind(chatMatchCreatedQueue)
                .to(matchExchange)
                .with(RabbitMQConstants.MATCH_CREATED_KEY);
    }

    /**
     * Binding for chat service to receive match ended events.
     */
    @Bean
    public Binding chatMatchEndedBinding(Queue chatMatchEndedQueue, TopicExchange matchExchange) {
        return BindingBuilder.bind(chatMatchEndedQueue)
                .to(matchExchange)
                .with(RabbitMQConstants.MATCH_ENDED_KEY);
    }

    /**
     * Binding for chat service to receive user deleted events.
     */
    @Bean
    public Binding chatUserDeletedBinding(Queue chatUserDeletedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(chatUserDeletedQueue)
                .to(userExchange)
                .with(RabbitMQConstants.USER_DELETED_KEY);
    }

    // ========================================
    // MESSAGE CONVERTER
    // ========================================

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
