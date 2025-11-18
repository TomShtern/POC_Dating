package com.dating.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkAndSet_NewKey_ReturnsTrue() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        boolean result = idempotencyService.checkAndSet("user1", "key1");

        assertTrue(result);
        verify(valueOperations).setIfAbsent(eq("idempotency:user1:key1"), eq("1"), any(Duration.class));
    }

    @Test
    void checkAndSet_ExistingKey_ReturnsFalse() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        boolean result = idempotencyService.checkAndSet("user1", "key1");

        assertFalse(result);
    }

    @Test
    void checkAndSet_NullKey_ReturnsTrue() {
        boolean result = idempotencyService.checkAndSet("user1", null);

        assertTrue(result);
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void checkAndSet_EmptyKey_ReturnsTrue() {
        boolean result = idempotencyService.checkAndSet("user1", "");

        assertTrue(result);
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void checkAndSet_RedisError_ReturnsTrue() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis error"));

        boolean result = idempotencyService.checkAndSet("user1", "key1");

        assertTrue(result); // Fails open
    }

    @Test
    void generateKey_ReturnsUniqueKeys() {
        String key1 = idempotencyService.generateKey();
        String key2 = idempotencyService.generateKey();

        assertNotNull(key1);
        assertNotNull(key2);
        assertNotEquals(key1, key2);
    }

    @Test
    void clear_DeletesKey() {
        idempotencyService.clear("user1", "key1");

        verify(redisTemplate).delete("idempotency:user1:key1");
    }

    @Test
    void clear_NullKey_DoesNothing() {
        idempotencyService.clear("user1", null);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void clear_EmptyKey_DoesNothing() {
        idempotencyService.clear("user1", "");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void clear_RedisError_DoesNotThrow() {
        doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete(anyString());

        assertDoesNotThrow(() -> idempotencyService.clear("user1", "key1"));
    }
}
