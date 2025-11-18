package com.dating.chat.dto.websocket;

/**
 * Message types for WebSocket communication.
 */
public enum MessageType {
    TEXT,           // Regular text message
    IMAGE,          // Image attachment
    TYPING,         // User is typing indicator
    READ_RECEIPT,   // Message was read
    DELIVERED,      // Message was delivered
    SYSTEM          // System messages (e.g., "User joined")
}
