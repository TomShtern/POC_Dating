package com.dating.chat.service;

import com.dating.chat.dto.websocket.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Push Notification Service
 *
 * Handles push notifications for offline users.
 * Stores push subscription tokens in Redis and sends notifications
 * via Firebase Cloud Messaging (for mobile) or Web Push (for browsers).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PresenceService presenceService;

    @Value("${app.push.enabled:false}")
    private boolean pushEnabled;

    private static final String PUSH_TOKEN_PREFIX = "push:token:";
    private static final String PUSH_SUBSCRIPTIONS_PREFIX = "push:subscriptions:";
    private static final Duration TOKEN_TTL = Duration.ofDays(30);

    /**
     * Register a push token for a user.
     *
     * @param userId The user ID
     * @param token The push token (FCM token for mobile, endpoint for web push)
     * @param platform The platform (fcm, web, apns)
     */
    public void registerPushToken(String userId, String token, String platform) {
        if (!pushEnabled) {
            log.debug("Push notifications disabled, skipping token registration");
            return;
        }

        try {
            String key = PUSH_TOKEN_PREFIX + userId + ":" + platform;
            redisTemplate.opsForValue().set(key, token, TOKEN_TTL);

            // Add to user's subscriptions set
            String subscriptionsKey = PUSH_SUBSCRIPTIONS_PREFIX + userId;
            redisTemplate.opsForSet().add(subscriptionsKey, platform);
            redisTemplate.expire(subscriptionsKey, TOKEN_TTL);

            log.info("Push token registered: userId={}, platform={}", userId, platform);
        } catch (Exception e) {
            log.error("Failed to register push token: userId={}", userId, e);
        }
    }

    /**
     * Unregister a push token.
     */
    public void unregisterPushToken(String userId, String platform) {
        try {
            String key = PUSH_TOKEN_PREFIX + userId + ":" + platform;
            redisTemplate.delete(key);

            String subscriptionsKey = PUSH_SUBSCRIPTIONS_PREFIX + userId;
            redisTemplate.opsForSet().remove(subscriptionsKey, platform);

            log.info("Push token unregistered: userId={}, platform={}", userId, platform);
        } catch (Exception e) {
            log.error("Failed to unregister push token: userId={}", userId, e);
        }
    }

    /**
     * Send push notification for a new message.
     * Only sends if the recipient is offline.
     */
    public void sendMessageNotification(UUID recipientId, ChatMessageEvent message) {
        if (!pushEnabled) {
            return;
        }

        // Only send push if user is offline
        if (presenceService.isOnline(recipientId.toString())) {
            log.debug("User {} is online, skipping push notification", recipientId);
            return;
        }

        try {
            // Get user's push subscriptions
            String subscriptionsKey = PUSH_SUBSCRIPTIONS_PREFIX + recipientId;
            Set<String> platforms = redisTemplate.opsForSet().members(subscriptionsKey);

            if (platforms == null || platforms.isEmpty()) {
                log.debug("No push subscriptions for user: {}", recipientId);
                return;
            }

            // Send to each platform
            for (String platform : platforms) {
                String tokenKey = PUSH_TOKEN_PREFIX + recipientId + ":" + platform;
                String token = redisTemplate.opsForValue().get(tokenKey);

                if (token != null) {
                    sendPushNotification(token, platform, message);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send push notification: recipientId={}", recipientId, e);
        }
    }

    /**
     * Send typing notification (optional, may be too noisy).
     */
    public void sendTypingNotification(UUID recipientId, String senderName) {
        // Typically not implemented as it would be too noisy
        // but could be used for important typing indicators
    }

    private void sendPushNotification(String token, String platform, ChatMessageEvent message) {
        // Build notification payload
        String title = "New message from " + message.senderName();
        String body = truncateMessage(message.content(), 100);

        switch (platform.toLowerCase()) {
            case "fcm" -> sendFcmNotification(token, title, body, message);
            case "web" -> sendWebPushNotification(token, title, body, message);
            case "apns" -> sendApnsNotification(token, title, body, message);
            default -> log.warn("Unknown push platform: {}", platform);
        }
    }

    /**
     * Send notification via Firebase Cloud Messaging.
     */
    private void sendFcmNotification(String token, String title, String body, ChatMessageEvent message) {
        // TODO: Implement FCM sending using Firebase Admin SDK
        // This requires firebase-admin dependency and configuration
        log.info("Would send FCM notification: token={}, title={}, body={}",
                truncateToken(token), title, body);

        // Example implementation:
        // FirebaseMessaging.getInstance().send(
        //     Message.builder()
        //         .setNotification(Notification.builder()
        //             .setTitle(title)
        //             .setBody(body)
        //             .build())
        //         .putData("matchId", message.matchId().toString())
        //         .putData("messageId", message.messageId().toString())
        //         .setToken(token)
        //         .build()
        // );
    }

    /**
     * Send Web Push notification.
     */
    private void sendWebPushNotification(String endpoint, String title, String body, ChatMessageEvent message) {
        // TODO: Implement Web Push using VAPID
        // This requires webpush-java dependency
        log.info("Would send Web Push notification: endpoint={}, title={}",
                truncateToken(endpoint), title);

        // Example implementation:
        // PushService pushService = new PushService(publicKey, privateKey);
        // Notification notification = new Notification(endpoint, publicKey, payload);
        // pushService.send(notification);
    }

    /**
     * Send notification via Apple Push Notification service.
     */
    private void sendApnsNotification(String token, String title, String body, ChatMessageEvent message) {
        // TODO: Implement APNS sending
        log.info("Would send APNS notification: token={}, title={}",
                truncateToken(token), title);
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return "";
        }
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength - 3) + "...";
    }

    private String truncateToken(String token) {
        if (token == null || token.length() <= 10) {
            return token;
        }
        return token.substring(0, 10) + "...";
    }

    /**
     * Check if push notifications are enabled.
     */
    public boolean isEnabled() {
        return pushEnabled;
    }
}
