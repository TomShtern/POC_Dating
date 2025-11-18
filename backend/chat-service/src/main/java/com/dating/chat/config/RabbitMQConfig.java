package com.dating.chat.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Chat Service.
 *
 * Defines queues, exchanges, and bindings for:
 * - Consuming match events (match.created, match.ended)
 * - Publishing chat events (message.sent, message.read)
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queues.match-created:chat.match.created.queue}")
    private String matchCreatedQueue;

    @Value("${app.rabbitmq.queues.match-ended:chat.match.ended.queue}")
    private String matchEndedQueue;

    @Value("${app.rabbitmq.exchange:match.exchange}")
    private String matchExchange;

    // --- Queues ---

    @Bean
    public Queue matchCreatedQueue() {
        return new Queue(matchCreatedQueue, true);
    }

    @Bean
    public Queue matchEndedQueue() {
        return new Queue(matchEndedQueue, true);
    }

    // --- Exchange ---

    @Bean
    public TopicExchange matchExchange() {
        return new TopicExchange(matchExchange);
    }

    // --- Bindings ---

    @Bean
    public Binding matchCreatedBinding(Queue matchCreatedQueue, TopicExchange matchExchange) {
        return BindingBuilder.bind(matchCreatedQueue)
                .to(matchExchange)
                .with("match.created");
    }

    @Bean
    public Binding matchEndedBinding(Queue matchEndedQueue, TopicExchange matchExchange) {
        return BindingBuilder.bind(matchEndedQueue)
                .to(matchExchange)
                .with("match.ended");
    }

    // --- Message Converter ---

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
