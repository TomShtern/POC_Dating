package com.dating.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(rateLimitService, "messagesPerSecond", 10);
        ReflectionTestUtils.setField(rateLimitService, "windowSeconds", 1);
        ReflectionTestUtils.setField(rateLimitService, "burstMultiplier", 1.5);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void isAllowed_WhenDisabled_ReturnsTrue() {
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        boolean result = rateLimitService.isAllowed("user1", "chat.send");

        assertTrue(result);
        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    void isAllowed_WhenUnderLimit_ReturnsTrue() {
        when(zSetOperations.zCard(anyString())).thenReturn(5L);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        boolean result = rateLimitService.isAllowed("user1", "chat.send");

        assertTrue(result);
        verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        verify(zSetOperations).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void isAllowed_WhenAtLimit_ReturnsFalse() {
        // 10 * 1.5 = 15 for chat.send
        when(zSetOperations.zCard(anyString())).thenReturn(15L);

        boolean result = rateLimitService.isAllowed("user1", "chat.send");

        assertFalse(result);
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void isAllowed_WhenRedisError_FailsClosed() {
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Redis error"));

        boolean result = rateLimitService.isAllowed("user1", "chat.send");

        assertFalse(result); // Fails closed for security
    }

    @Test
    void getRemainingQuota_ReturnsCorrectValue() {
        when(zSetOperations.zCard(anyString())).thenReturn(5L);

        int remaining = rateLimitService.getRemainingQuota("user1", "chat.send");

        assertEquals(10, remaining); // 15 - 5
    }

    @Test
    void getRemainingQuota_WhenDisabled_ReturnsMaxValue() {
        ReflectionTestUtils.setField(rateLimitService, "rateLimitEnabled", false);

        int remaining = rateLimitService.getRemainingQuota("user1", "chat.send");

        assertEquals(Integer.MAX_VALUE, remaining);
    }

    @Test
    void isAllowed_DifferentDestinations_HaveDifferentLimits() {
        // chat.typing has 2x multiplier
        when(zSetOperations.zCard(anyString())).thenReturn(20L);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        boolean result = rateLimitService.isAllowed("user1", "chat.typing");

        assertTrue(result); // 10 * 1.5 * 2 = 30 > 20
    }
}
