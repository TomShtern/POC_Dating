package com.dating.chat.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration using Redis.
 * Defines cache names and TTL for chat-related data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names
    public static final String MESSAGES_CACHE = "messages";
    public static final String CONVERSATIONS_CACHE = "conversations";
    public static final String CONVERSATION_MESSAGES_CACHE = "conversation_messages";
    public static final String UNREAD_COUNT_CACHE = "unread_counts";

    // Cache TTL (in minutes)
    private static final long MESSAGES_TTL = 60;           // 1 hour
    private static final long CONVERSATIONS_TTL = 30;      // 30 minutes
    private static final long UNREAD_COUNT_TTL = 15;       // 15 minutes

    /**
     * Configure Redis cache manager with specific TTL per cache.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Messages cache - 1 hour TTL
        cacheConfigurations.put(MESSAGES_CACHE, defaultConfig
                .entryTtl(Duration.ofMinutes(MESSAGES_TTL)));

        // Conversations list cache - 30 minutes TTL
        cacheConfigurations.put(CONVERSATIONS_CACHE, defaultConfig
                .entryTtl(Duration.ofMinutes(CONVERSATIONS_TTL)));

        // Conversation messages cache - 1 hour TTL
        cacheConfigurations.put(CONVERSATION_MESSAGES_CACHE, defaultConfig
                .entryTtl(Duration.ofMinutes(MESSAGES_TTL)));

        // Unread count cache - 15 minutes TTL
        cacheConfigurations.put(UNREAD_COUNT_CACHE, defaultConfig
                .entryTtl(Duration.ofMinutes(UNREAD_COUNT_TTL)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
