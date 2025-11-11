# Chat Service

## Overview

Microservice providing real-time messaging capabilities for matched users via WebSockets.

## Port
**8083** (internal, accessed via API Gateway)

## Responsibilities

### Real-Time Messaging
- WebSocket server for instant messaging
- Message delivery confirmation
- Read receipts
- Typing indicators
- Online/offline status

### Message Management
- Store message history
- Retrieve conversation threads
- Search messages
- Message deletion (soft-delete)

### Conversation Management
- Create conversations for matches
- Archive conversations
- Conversation metadata (last message, timestamp)

## Database Schema

```
conversations
├── id (UUID, PK)
├── match_id (FK)
├── user1_id (FK)
├── user2_id (FK)
├── created_at
├── archived_at (nullable)
└── UNIQUE(match_id)

messages
├── id (UUID, PK)
├── conversation_id (FK)
├── sender_id (FK)
├── content (TEXT)
├── status (SENT, DELIVERED, READ)
├── created_at
├── read_at (nullable)
├── deleted_at (nullable)
└── INDEX(conversation_id, created_at)

typing_status
├── user_id (FK)
├── conversation_id (FK)
├── started_at
└── UNIQUE(user_id, conversation_id)
```

## WebSocket Events

### From Client
```
CONNECT
├── Authentication via JWT
└── Subscribe to user's conversations

SEND_MESSAGE
├── Payload: ConversationId, Content, Timestamp
└── Broadcast to recipient & persist

MARK_AS_READ
├── Payload: ConversationId, MessageIds
└── Update message status

TYPING_START
├── Payload: ConversationId
└── Broadcast to other user (no persistence)

TYPING_STOP
├── Payload: ConversationId
└── Broadcast to other user
```

### To Client
```
MESSAGE_RECEIVED
├── MessageId, SenderId, Content, Timestamp
└── Sent to conversation subscribers

MESSAGE_DELIVERED
├── MessageId, DeliveredAt
└── Confirmation to sender

MESSAGE_READ
├── MessageId, ReadAt
└── Confirmation to sender

USER_ONLINE
├── ConversationId, UserId
└── Typing indicator cleanup

USER_OFFLINE
├── ConversationId, UserId
└── Status update
```

## REST API Endpoints

**Base URL:** http://localhost:8080/api/chat

### Conversation Management
```
GET    /conversations       → Get all user's conversations
GET    /conversations/{id}  → Get conversation details
GET    /conversations/{id}/messages → Get message history (paginated)
DELETE /conversations/{id}  → Archive conversation
```

### Message Operations
```
GET    /messages/{id}       → Get specific message
DELETE /messages/{id}       → Delete message
PUT    /messages/{id}/read  → Mark as read
```

## WebSocket Connection

```
WebSocket URL: ws://localhost:8080/api/chat/ws

Connection Header:
Authorization: Bearer {JWT_TOKEN}

Response:
{
  "type": "CONNECT_SUCCESS",
  "userId": "uuid",
  "conversations": ["convo-id-1", "convo-id-2"]
}
```

## Scaling Considerations

### Problem: Single WebSocket Server
**Solution:** RabbitMQ with STOMP broker relay
- Messages route through RabbitMQ to all service instances
- Allows horizontal scaling

### Problem: WebSocket Connection State
**Solution:** Redis for session state
- Track active connections per user
- Publish/Subscribe for multi-instance messaging

### Problem: High Message Volume
**Solution:** Database batching
- Batch write messages every 100ms
- Reduces transaction overhead

## Caching Strategy

```
Redis Keys:
├── ws:user:{userId}:status      → Online/Offline (TTL: connection lifetime)
├── ws:conversation:{id}:users   → Active users in convo (TTL: connection)
├── typing:{conversationId}      → Current typing users (TTL: 5s)
├── messages:{conversationId}    → Recent messages (TTL: 1h, 50 newest)
└── unread:{userId}              → Unread count (TTL: until read)
```

## Error Handling

```
400 Bad Request      → Invalid message format or missing fields
401 Unauthorized     → Invalid/expired JWT
403 Forbidden        → Not participant in conversation
404 Not Found        → Conversation/message not found
429 Too Many Requests→ Message rate limit
500 Internal Server  → Database or WebSocket error
```

## Performance Metrics

- Message delivery latency: < 100ms (p95)
- WebSocket connection setup: < 200ms
- Typing indicator broadcast: < 50ms
- Message history retrieval: < 500ms for 100 messages

## Testing

### Unit Tests
- Message validation
- Status transitions
- Permission checks

### Integration Tests
- WebSocket connections
- Message delivery
- Read receipt flow
- RabbitMQ integration
- Database persistence

### Load Testing
- Concurrent WebSocket connections
- Message throughput
- Latency under load

## Security

- JWT authentication on WebSocket
- Rate limiting (10 messages/second per user)
- Input sanitization
- Message encryption (future)
- Audit logging of deletions

## Future Enhancements

- Message encryption end-to-end
- Image/media sharing
- Voice/video calling integration
- Message reactions (emoji)
- Forward messages
- Edit message history
- Message search with full-text
- Message pinning
- Conversation search and filtering
- Auto-delete old messages
- Message expiration (Snapchat-like)
