package com.dating.common.actuator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Custom actuator endpoint for cache statistics.
 * Provides detailed information about cache operations and keys.
 */
@Component
@Endpoint(id = "cacheStats")
@RequiredArgsConstructor
@Slf4j
public class CacheStatisticsEndpoint {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get statistics for all caches.
     *
     * @return Map of cache statistics
     */
    @ReadOperation
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();

        statistics.put("totalCaches", cacheNames.size());

        Map<String, Object> caches = new HashMap<>();
        for (String cacheName : cacheNames) {
            caches.put(cacheName, getCacheInfo(cacheName));
        }
        statistics.put("caches", caches);

        return statistics;
    }

    /**
     * Get statistics for a specific cache.
     *
     * @param cacheName Cache name
     * @return Cache statistics
     */
    @ReadOperation
    public Map<String, Object> getCacheStatistics(@Selector String cacheName) {
        return getCacheInfo(cacheName);
    }

    private Map<String, Object> getCacheInfo(String cacheName) {
        Map<String, Object> info = new HashMap<>();
        Cache cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            info.put("status", "NOT_FOUND");
            return info;
        }

        info.put("name", cacheName);
        info.put("type", cache.getClass().getSimpleName());

        if (cache instanceof RedisCache) {
            // Get Redis-specific statistics
            try {
                String keyPattern = cacheName + "::*";
                Set<String> keys = redisTemplate.keys(keyPattern);
                info.put("approximateKeyCount", keys != null ? keys.size() : 0);
                info.put("status", "ACTIVE");
            } catch (Exception e) {
                log.warn("Failed to get Redis cache info for {}: {}", cacheName, e.getMessage());
                info.put("status", "ERROR");
                info.put("error", e.getMessage());
            }
        } else {
            info.put("status", "ACTIVE");
        }

        return info;
    }
}
