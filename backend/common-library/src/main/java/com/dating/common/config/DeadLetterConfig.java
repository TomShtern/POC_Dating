package com.dating.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dead Letter Queue configuration for RabbitMQ.
 * Handles failed messages that cannot be processed.
 */
@Configuration
public class DeadLetterConfig {

    /**
     * Dead letter exchange for failed messages.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConstants.DEAD_LETTER_EXCHANGE);
    }

    /**
     * Dead letter queue for storing failed messages.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstants.DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * Binding between dead letter queue and exchange.
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.DEAD_LETTER_KEY);
    }

    /**
     * Helper method to create queue arguments for dead letter routing.
     * Services should use this when creating their queues.
     *
     * @return Arguments map for dead letter configuration
     */
    public static java.util.Map<String, Object> getDeadLetterArguments() {
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("x-dead-letter-exchange", RabbitMQConstants.DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", RabbitMQConstants.DEAD_LETTER_KEY);
        return args;
    }
}
