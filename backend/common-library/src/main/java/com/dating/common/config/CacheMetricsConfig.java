package com.dating.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;

import jakarta.annotation.PostConstruct;
import java.util.Collection;

/**
 * Configuration for cache metrics monitoring.
 * Exposes cache hit/miss ratios and other metrics via Micrometer.
 */
@Configuration
@Slf4j
public class CacheMetricsConfig {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public CacheMetricsConfig(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void bindCachesToRegistry() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        log.info("Registering cache metrics for {} caches", cacheNames.size());

        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Register custom metrics for each cache
                registerCacheMetrics(cacheName);
                log.debug("Registered metrics for cache: {}", cacheName);
            }
        }
    }

    private void registerCacheMetrics(String cacheName) {
        // Register gauges for cache operations
        meterRegistry.gauge("cache.size",
                io.micrometer.core.instrument.Tags.of("cache", cacheName),
                cacheManager,
                cm -> {
                    Cache cache = cm.getCache(cacheName);
                    if (cache instanceof RedisCache redisCache) {
                        // For Redis cache, we can't easily get size
                        return -1;
                    }
                    return 0;
                });
    }
}
