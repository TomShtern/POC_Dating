package com.dating.user.config;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration.
 * Defines cache names and TTL for different cache types.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names
    public static final String USERS_CACHE = "users";
    public static final String USER_PREFERENCES_CACHE = "user_preferences";
    public static final String USER_BY_EMAIL_CACHE = "users_by_email";

    // TTL values
    private static final Duration USER_CACHE_TTL = Duration.ofHours(1);
    private static final Duration PREFERENCES_CACHE_TTL = Duration.ofHours(1);

    /**
     * Configure Redis cache manager with specific TTL for each cache.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(USERS_CACHE, defaultConfig.entryTtl(USER_CACHE_TTL));
        cacheConfigurations.put(USER_PREFERENCES_CACHE, defaultConfig.entryTtl(PREFERENCES_CACHE_TTL));
        cacheConfigurations.put(USER_BY_EMAIL_CACHE, defaultConfig.entryTtl(USER_CACHE_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
