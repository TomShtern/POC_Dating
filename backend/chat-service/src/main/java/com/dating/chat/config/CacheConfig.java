package com.dating.chat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache Configuration
 *
 * Configures Redis-based caching with proper serialization.

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

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure ObjectMapper for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Configure specific cache TTLs
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .withCacheConfiguration("messages",
                        cacheConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("conversations",
                        cacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("users",
                        cacheConfig.entryTtl(Duration.ofHours(1)))
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
