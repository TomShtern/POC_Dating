package com.dating.match.config;

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
 * Redis cache configuration for Match Service.
 * Defines cache names and TTL for different cache types.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names
    public static final String FEED_CACHE = "feed";
    public static final String MATCHES_CACHE = "matches";
    public static final String MATCH_DETAILS_CACHE = "match_details";

    // TTL values
    private static final Duration FEED_CACHE_TTL = Duration.ofHours(24);
    private static final Duration MATCHES_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration MATCH_DETAILS_CACHE_TTL = Duration.ofHours(1);

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

        cacheConfigurations.put(FEED_CACHE, defaultConfig.entryTtl(FEED_CACHE_TTL));
        cacheConfigurations.put(MATCHES_CACHE, defaultConfig.entryTtl(MATCHES_CACHE_TTL));
        cacheConfigurations.put(MATCH_DETAILS_CACHE, defaultConfig.entryTtl(MATCH_DETAILS_CACHE_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
