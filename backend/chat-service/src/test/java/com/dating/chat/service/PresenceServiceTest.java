package com.dating.chat.service;

import com.dating.chat.dto.websocket.PresenceChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(redisTemplate, messagingTemplate);
        ReflectionTestUtils.setField(presenceService, "presenceTtlSeconds", 1800L);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void setOnline_FirstSession_MarksUserOnline() {
        when(setOperations.size(anyString())).thenReturn(0L);

        boolean wasOffline = presenceService.setOnline("user1", "session1", "John");

        assertTrue(wasOffline);
        verify(setOperations).add(contains("sessions"), eq("session1"));
        verify(setOperations).add("presence:online", "user1");
        verify(messagingTemplate).convertAndSend(eq("/topic/presence"), any(PresenceChangeEvent.class));
    }

    @Test
    void setOnline_SecondSession_DoesNotBroadcast() {
        when(setOperations.size(anyString())).thenReturn(1L);

        boolean wasOffline = presenceService.setOnline("user1", "session2", "John");

        assertFalse(wasOffline);
        verify(setOperations).add(contains("sessions"), eq("session2"));
        verify(setOperations, never()).add("presence:online", "user1");
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(PresenceChangeEvent.class));
    }

    @Test
    void setOffline_LastSession_MarksUserOffline() {
        when(setOperations.size(anyString())).thenReturn(0L);

        boolean fullyOffline = presenceService.setOffline("user1", "session1");

        assertTrue(fullyOffline);
        verify(setOperations).remove(contains("sessions"), eq("session1"));
        verify(setOperations).remove("presence:online", "user1");
        verify(messagingTemplate).convertAndSend(eq("/topic/presence"), any(PresenceChangeEvent.class));
    }

    @Test
    void setOffline_StillHasSessions_DoesNotBroadcast() {
        when(setOperations.size(anyString())).thenReturn(1L);

        boolean fullyOffline = presenceService.setOffline("user1", "session1");

        assertFalse(fullyOffline);
        verify(setOperations).remove(contains("sessions"), eq("session1"));
        verify(setOperations, never()).remove("presence:online", "user1");
    }

    @Test
    void isOnline_ReturnsTrueWhenOnline() {
        when(setOperations.isMember("presence:online", "user1")).thenReturn(true);

        boolean online = presenceService.isOnline("user1");

        assertTrue(online);
    }

    @Test
    void isOnline_ReturnsFalseWhenOffline() {
        when(setOperations.isMember("presence:online", "user1")).thenReturn(false);

        boolean online = presenceService.isOnline("user1");

        assertFalse(online);
    }

    @Test
    void getOnlineUsers_ReturnsSet() {
        Set<String> expected = Set.of("user1", "user2");
        when(setOperations.members("presence:online")).thenReturn(expected);

        Set<String> result = presenceService.getOnlineUsers();

        assertEquals(expected, result);
    }

    @Test
    void getOnlineUsers_ReturnsEmptyOnError() {
        when(setOperations.members("presence:online")).thenThrow(new RuntimeException("Redis error"));

        Set<String> result = presenceService.getOnlineUsers();

        assertTrue(result.isEmpty());
    }

    @Test
    void getSessionCount_ReturnsCount() {
        when(setOperations.size(anyString())).thenReturn(3L);

        long count = presenceService.getSessionCount("user1");

        assertEquals(3, count);
    }

    @Test
    void setOnline_BroadcastsCorrectEvent() {
        when(setOperations.size(anyString())).thenReturn(0L);

        presenceService.setOnline("user1", "session1", "John");

        ArgumentCaptor<PresenceChangeEvent> captor = ArgumentCaptor.forClass(PresenceChangeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/presence"), captor.capture());

        PresenceChangeEvent event = captor.getValue();
        assertEquals("user1", event.userId());
        assertEquals("John", event.username());
        assertTrue(event.isOnline());
        assertNotNull(event.timestamp());
    }
}
