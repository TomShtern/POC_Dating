package com.dating.chat.service;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import com.dating.chat.dto.websocket.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private PresenceService presenceService;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PushNotificationService pushNotificationService;

    @BeforeEach
    void setUp() {
        pushNotificationService = new PushNotificationService(redisTemplate, presenceService);
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", true);

        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void registerPushToken_Success() {
        pushNotificationService.registerPushToken("user1", "token123", "fcm");

        verify(valueOperations).set(eq("push:token:user1:fcm"), eq("token123"), any(Duration.class));
        verify(setOperations).add("push:subscriptions:user1", "fcm");
    }

    @Test
    void registerPushToken_WhenDisabled_DoesNothing() {
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", false);

        pushNotificationService.registerPushToken("user1", "token123", "fcm");

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void unregisterPushToken_DeletesToken() {
        pushNotificationService.unregisterPushToken("user1", "fcm");

        verify(redisTemplate).delete("push:token:user1:fcm");
        verify(setOperations).remove("push:subscriptions:user1", "fcm");
    }

    @Test
    void sendMessageNotification_WhenUserOnline_SkipsNotification() {
        UUID recipientId = UUID.randomUUID();
        ChatMessageEvent message = createTestMessage();

        when(presenceService.isOnline(recipientId.toString())).thenReturn(true);

        pushNotificationService.sendMessageNotification(recipientId, message);

        verify(setOperations, never()).members(anyString());
    }

    @Test
    void sendMessageNotification_WhenUserOffline_SendsNotification() {
        UUID recipientId = UUID.randomUUID();
        ChatMessageEvent message = createTestMessage();

        when(presenceService.isOnline(recipientId.toString())).thenReturn(false);
        when(setOperations.members("push:subscriptions:" + recipientId)).thenReturn(Set.of("fcm"));
        when(valueOperations.get("push:token:" + recipientId + ":fcm")).thenReturn("token123");

        pushNotificationService.sendMessageNotification(recipientId, message);

        verify(setOperations).members("push:subscriptions:" + recipientId);
        verify(valueOperations).get("push:token:" + recipientId + ":fcm");
    }

    @Test
    void sendMessageNotification_WhenDisabled_DoesNothing() {
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", false);

        pushNotificationService.sendMessageNotification(UUID.randomUUID(), createTestMessage());

        verify(presenceService, never()).isOnline(anyString());
    }

    @Test
    void sendMessageNotification_NoSubscriptions_DoesNothing() {
        UUID recipientId = UUID.randomUUID();

        when(presenceService.isOnline(recipientId.toString())).thenReturn(false);
        when(setOperations.members("push:subscriptions:" + recipientId)).thenReturn(null);

        pushNotificationService.sendMessageNotification(recipientId, createTestMessage());

        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void sendMessageNotification_MultipleSubscriptions_SendsToAll() {
        UUID recipientId = UUID.randomUUID();
        ChatMessageEvent message = createTestMessage();

        when(presenceService.isOnline(recipientId.toString())).thenReturn(false);
        when(setOperations.members("push:subscriptions:" + recipientId))
                .thenReturn(Set.of("fcm", "web"));
        when(valueOperations.get("push:token:" + recipientId + ":fcm")).thenReturn("fcm-token");
        when(valueOperations.get("push:token:" + recipientId + ":web")).thenReturn("web-endpoint");

        pushNotificationService.sendMessageNotification(recipientId, message);

        verify(valueOperations).get("push:token:" + recipientId + ":fcm");
        verify(valueOperations).get("push:token:" + recipientId + ":web");
    }

    @Test
    void isEnabled_ReturnsCorrectValue() {
        assertTrue(pushNotificationService.isEnabled());

        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", false);
        assertFalse(pushNotificationService.isEnabled());
    }

    @Test
    void registerPushToken_RedisError_DoesNotThrow() {
        doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertDoesNotThrow(() ->
                pushNotificationService.registerPushToken("user1", "token", "fcm"));
    }

    private ChatMessageEvent createTestMessage() {
        return new ChatMessageEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "John",
                "Hello, this is a test message",
                MessageType.TEXT,
                Instant.now()
        );
    }
}
