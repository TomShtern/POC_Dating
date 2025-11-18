package com.dating.ui.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking page view metrics.
 * Provides counters for different pages in the Vaadin UI.
 */
@Service
@Slf4j
public class PageViewMetricsService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> pageCounters = new ConcurrentHashMap<>();

    public PageViewMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record a page view for the given page name.
     *
     * @param pageName the name of the page (e.g., "login", "discover", "matches", "chat", "profile")
     */
    public void recordPageView(String pageName) {
        Counter counter = pageCounters.computeIfAbsent(pageName, this::createCounter);
        counter.increment();
        log.debug("Page view recorded: {}", pageName);
    }

    private Counter createCounter(String pageName) {
        return Counter.builder("ui.page.views.total")
            .description("Total number of page views")
            .tag("page", pageName)
            .register(meterRegistry);
    }
}
