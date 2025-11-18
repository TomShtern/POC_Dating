package com.dating.recommendation.config;

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
    public static final String RECOMMENDATIONS_CACHE = "recommendations";
    public static final String COMPATIBILITY_SCORES_CACHE = "compatibility_scores";
    public static final String USER_PROFILES_CACHE = "user_profiles";

    // TTL values
    private static final Duration RECOMMENDATIONS_CACHE_TTL = Duration.ofHours(24);
    private static final Duration COMPATIBILITY_CACHE_TTL = Duration.ofHours(24);
    private static final Duration USER_PROFILES_CACHE_TTL = Duration.ofHours(1);

    /**
     * Configure Redis cache manager with specific TTL for each cache.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(RECOMMENDATIONS_CACHE,
                defaultConfig.entryTtl(RECOMMENDATIONS_CACHE_TTL));
        cacheConfigurations.put(COMPATIBILITY_SCORES_CACHE,
                defaultConfig.entryTtl(COMPATIBILITY_CACHE_TTL));
        cacheConfigurations.put(USER_PROFILES_CACHE,
                defaultConfig.entryTtl(USER_PROFILES_CACHE_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
