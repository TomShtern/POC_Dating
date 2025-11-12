# Chat Service

## Overview

Microservice providing real-time messaging capabilities for matched users via WebSockets. Built with Spring Boot 3.2.0, WebSocket/STOMP, PostgreSQL, Redis, and RabbitMQ.

## Port
**8083** (internal, accessed via API Gateway at port 8080)

## Prerequisites

### Required (for Default PostgreSQL Setup)
- **Java 21+** - Required for running the service
- **Maven 3.8+** - Build tool
- **PostgreSQL 14+** - Default database (local installation)

#### PostgreSQL Installation

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**macOS (Homebrew):**
```bash
brew install postgresql@14
brew services start postgresql@14
```

**Windows:**
Download and install from [postgresql.org](https://www.postgresql.org/download/windows/)

**Create Database:**
```bash
# Connect to PostgreSQL as postgres user
sudo -u postgres psql

# Or on Windows/Mac (if postgres user has no password):
psql -U postgres

# Create the database
CREATE DATABASE dating_chat;

# Exit psql
\q
```

Or use the quick setup script: `/home/user/POC_Dating/backend/setup-databases.sql`

### Optional Services (Not Required for Basic Functionality)
- **Redis 7+** - For WebSocket session management (optional, not required for basic chat)
- **RabbitMQ 3.12+** - For distributed WebSocket STOMP relay (optional)
- **Docker** - For containerized deployment (optional)

## Technology Stack

- **Spring Boot 3.2.0** - Core framework
- **Spring WebSocket + STOMP** - Real-time bidirectional communication
- **PostgreSQL** - Message and conversation persistence
- **Redis** - Session management and caching
- **RabbitMQ** - Message broker for distributed WebSocket messaging
- **Spring Cloud OpenFeign** - Inter-service communication
- **JWT** - Authentication and authorization
- **Lombok** - Reduce boilerplate code

## Key Features

### Real-Time Messaging
- WebSocket server for instant messaging with STOMP protocol
- Message delivery confirmation and read receipts
- Typing indicators (ephemeral, no persistence)
- Online/offline status tracking
- Message broadcasting to recipients

### Message Management
- Persistent message storage in PostgreSQL
- Message history retrieval (paginated)
- Soft-delete messages (sender only)
- Message status tracking (SENT, DELIVERED, READ)

### Conversation Management
- Automatic conversation creation from matches
- Archive conversations
- Conversation metadata (last message, timestamp)
- Unread message count per conversation

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

**Base URL:** `http://localhost:8080/api/chat` (via API Gateway)
**Direct URL:** `http://localhost:8083/api/chat`

All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

### Health Check
```http
GET /api/chat/health
```
**Response:**
```json
"Chat service is running"
```

---

### Conversation Management

#### 1. Get All Conversations
```http
GET /api/chat/conversations
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "matchId": "223e4567-e89b-12d3-a456-426614174001",
    "createdAt": "2024-01-15T10:30:00",
    "lastMessageAt": "2024-01-15T14:20:00",
    "otherUserId": "323e4567-e89b-12d3-a456-426614174002",
    "otherUserName": "Jane Doe",
    "otherUserPhotoUrl": "https://example.com/photos/jane.jpg",
    "lastMessageContent": "Hey! How are you?",
    "lastMessageTimestamp": "2024-01-15T14:20:00",
    "lastMessageFromMe": false,
    "unreadCount": 3
  }
]
```

---

#### 2. Get Conversation by ID
```http
GET /api/chat/conversations/{conversationId}
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "user1Id": "323e4567-e89b-12d3-a456-426614174002",
  "user2Id": "423e4567-e89b-12d3-a456-426614174003",
  "matchId": "223e4567-e89b-12d3-a456-426614174001",
  "createdAt": "2024-01-15T10:30:00",
  "lastMessageAt": "2024-01-15T14:20:00",
  "archived": false,
  "otherUser": {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "name": "Jane Doe",
    "email": "jane@example.com",
    "age": 28,
    "photoUrl": "https://example.com/photos/jane.jpg",
    "bio": "Love hiking and coffee"
  },
  "lastMessage": {
    "id": "523e4567-e89b-12d3-a456-426614174004",
    "conversationId": "123e4567-e89b-12d3-a456-426614174000",
    "senderId": "323e4567-e89b-12d3-a456-426614174002",
    "recipientId": "423e4567-e89b-12d3-a456-426614174003",
    "content": "Hey! How are you?",
    "timestamp": "2024-01-15T14:20:00",
    "status": "DELIVERED",
    "deliveredAt": "2024-01-15T14:20:05",
    "readAt": null,
    "deleted": false
  }
}
```

**Errors:**
- `404 Not Found` - Conversation not found
- `403 Forbidden` - User is not a participant

---

#### 3. Create Conversation from Match
```http
POST /api/chat/conversations/from-match/{matchId}
Authorization: Bearer <jwt_token>
```

**Response:** `201 Created`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "user1Id": "323e4567-e89b-12d3-a456-426614174002",
  "user2Id": "423e4567-e89b-12d3-a456-426614174003",
  "matchId": "223e4567-e89b-12d3-a456-426614174001",
  "createdAt": "2024-01-15T10:30:00",
  "lastMessageAt": null,
  "archived": false
}
```

**Errors:**
- `400 Bad Request` - Match not found
- `403 Forbidden` - User is not part of the match
- `409 Conflict` - Conversation already exists for this match

---

#### 4. Archive Conversation
```http
DELETE /api/chat/conversations/{conversationId}
Authorization: Bearer <jwt_token>
```

**Response:** `204 No Content`

**Errors:**
- `404 Not Found` - Conversation not found
- `403 Forbidden` - User is not a participant

---

### Message Operations

#### 5. Get Message History (Paginated)
```http
GET /api/chat/conversations/{conversationId}/messages?page=0&size=50
Authorization: Bearer <jwt_token>
```

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 50) - Page size

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "523e4567-e89b-12d3-a456-426614174004",
      "conversationId": "123e4567-e89b-12d3-a456-426614174000",
      "senderId": "323e4567-e89b-12d3-a456-426614174002",
      "recipientId": "423e4567-e89b-12d3-a456-426614174003",
      "content": "Hey! How are you?",
      "timestamp": "2024-01-15T14:20:00",
      "status": "READ",
      "deliveredAt": "2024-01-15T14:20:05",
      "readAt": "2024-01-15T14:25:00",
      "deleted": false
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50
  },
  "totalElements": 127,
  "totalPages": 3,
  "last": false
}
```

---

#### 6. Get Recent Messages
```http
GET /api/chat/conversations/{conversationId}/messages/recent?limit=50
Authorization: Bearer <jwt_token>
```

**Query Parameters:**
- `limit` (optional, default: 50) - Number of recent messages

**Response:** `200 OK`
```json
[
  {
    "id": "523e4567-e89b-12d3-a456-426614174004",
    "conversationId": "123e4567-e89b-12d3-a456-426614174000",
    "senderId": "323e4567-e89b-12d3-a456-426614174002",
    "recipientId": "423e4567-e89b-12d3-a456-426614174003",
    "content": "Hey! How are you?",
    "timestamp": "2024-01-15T14:20:00",
    "status": "READ",
    "deliveredAt": "2024-01-15T14:20:05",
    "readAt": "2024-01-15T14:25:00",
    "deleted": false
  }
]
```

---

#### 7. Send Message (REST Alternative)
```http
POST /api/chat/messages
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "content": "Hello! Nice to meet you!"
}
```

**Response:** `201 Created`
```json
{
  "id": "623e4567-e89b-12d3-a456-426614174005",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "senderId": "423e4567-e89b-12d3-a456-426614174003",
  "recipientId": "323e4567-e89b-12d3-a456-426614174002",
  "content": "Hello! Nice to meet you!",
  "timestamp": "2024-01-15T14:30:00",
  "status": "SENT",
  "deliveredAt": null,
  "readAt": null,
  "deleted": false
}
```

**Validation:**
- `conversationId` - Required
- `content` - Required, 1-2000 characters

**Errors:**
- `400 Bad Request` - Invalid request body
- `403 Forbidden` - User is not a participant

---

#### 8. Get Message by ID
```http
GET /api/chat/messages/{messageId}
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`
```json
{
  "id": "523e4567-e89b-12d3-a456-426614174004",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "senderId": "323e4567-e89b-12d3-a456-426614174002",
  "recipientId": "423e4567-e89b-12d3-a456-426614174003",
  "content": "Hey! How are you?",
  "timestamp": "2024-01-15T14:20:00",
  "status": "READ",
  "deliveredAt": "2024-01-15T14:20:05",
  "readAt": "2024-01-15T14:25:00",
  "deleted": false
}
```

---

#### 9. Mark Messages as Read
```http
PUT /api/chat/messages/read
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "messageIds": [
    "523e4567-e89b-12d3-a456-426614174004",
    "623e4567-e89b-12d3-a456-426614174005"
  ]
}
```

**Response:** `204 No Content`

---

#### 10. Delete Message
```http
DELETE /api/chat/messages/{messageId}
Authorization: Bearer <jwt_token>
```

**Response:** `204 No Content`

**Note:** Only the message sender can delete their own messages (soft delete).

**Errors:**
- `404 Not Found` - Message not found
- `403 Forbidden` - User is not the sender

## WebSocket Real-Time Messaging

### Connection Setup

**WebSocket URL:** `ws://localhost:8080/ws` (via API Gateway)
**Direct URL:** `ws://localhost:8083/ws`

**Protocol:** STOMP over WebSocket (with SockJS fallback)

### JavaScript Client Example

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

// Create WebSocket connection
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// JWT token from authentication
const jwtToken = 'your-jwt-token-here';

// Connect with authentication
stompClient.connect(
  { Authorization: `Bearer ${jwtToken}` },
  (frame) => {
    console.log('Connected: ' + frame);

    // Subscribe to personal message queue
    stompClient.subscribe('/user/queue/messages', (message) => {
      const msg = JSON.parse(message.body);
      console.log('Received message:', msg);
      handleIncomingMessage(msg);
    });

    // Subscribe to notifications (delivery/read receipts)
    stompClient.subscribe('/user/queue/notifications', (notification) => {
      const notif = JSON.parse(notification.body);
      console.log('Notification:', notif);
      handleNotification(notif);
    });

    // Subscribe to typing indicators for a conversation
    const conversationId = '123e4567-e89b-12d3-a456-426614174000';
    stompClient.subscribe(`/topic/conversation/${conversationId}/typing`, (typing) => {
      const typingData = JSON.parse(typing.body);
      console.log('Typing indicator:', typingData);
      handleTypingIndicator(typingData);
    });

    // Subscribe to user status updates
    stompClient.subscribe('/topic/users/status', (status) => {
      const statusData = JSON.parse(status.body);
      console.log('User status:', statusData);
      handleUserStatus(statusData);
    });
  },
  (error) => {
    console.error('WebSocket error:', error);
  }
);
```

---

### WebSocket Message Types

#### 1. Send Message

**Client → Server:** `/app/chat.send`

```javascript
stompClient.send('/app/chat.send', {}, JSON.stringify({
  conversationId: '123e4567-e89b-12d3-a456-426614174000',
  content: 'Hello! How are you?'
}));
```

**Server → Recipient:** `/user/queue/messages`

```json
{
  "type": "MESSAGE_RECEIVED",
  "messageId": "523e4567-e89b-12d3-a456-426614174004",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "senderId": "323e4567-e89b-12d3-a456-426614174002",
  "recipientId": "423e4567-e89b-12d3-a456-426614174003",
  "content": "Hello! How are you?",
  "timestamp": "2024-01-15T14:30:00"
}
```

**Server → Sender:** `/user/queue/notifications` (confirmation)

```json
{
  "type": "MESSAGE_SENT",
  "messageId": "523e4567-e89b-12d3-a456-426614174004",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "senderId": "323e4567-e89b-12d3-a456-426614174002",
  "recipientId": "423e4567-e89b-12d3-a456-426614174003",
  "content": "Hello! How are you?",
  "timestamp": "2024-01-15T14:30:00"
}
```

---

#### 2. Mark Messages as Read

**Client → Server:** `/app/chat.markRead`

```javascript
stompClient.send('/app/chat.markRead', {}, JSON.stringify({
  conversationId: '123e4567-e89b-12d3-a456-426614174000',
  messageIds: [
    '523e4567-e89b-12d3-a456-426614174004',
    '623e4567-e89b-12d3-a456-426614174005'
  ]
}));
```

**Server → Original Sender:** `/user/queue/notifications`

```json
{
  "type": "MESSAGE_READ",
  "messageId": "523e4567-e89b-12d3-a456-426614174004",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2024-01-15T14:35:00"
}
```

---

#### 3. Message Delivered Acknowledgment

**Client → Server:** `/app/chat.delivered`

```javascript
stompClient.send('/app/chat.delivered', {}, JSON.stringify({
  messageId: '523e4567-e89b-12d3-a456-426614174004'
}));
```

**Server → Original Sender:** `/user/queue/notifications`

```json
{
  "type": "MESSAGE_DELIVERED",
  "messageId": "523e4567-e89b-12d3-a456-426614174004",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2024-01-15T14:30:05"
}
```

---

#### 4. Typing Indicators

**Start Typing - Client → Server:** `/app/chat.typing.start`

```javascript
stompClient.send('/app/chat.typing.start', {}, JSON.stringify({
  conversationId: '123e4567-e89b-12d3-a456-426614174000'
}));
```

**Stop Typing - Client → Server:** `/app/chat.typing.stop`

```javascript
stompClient.send('/app/chat.typing.stop', {}, JSON.stringify({
  conversationId: '123e4567-e89b-12d3-a456-426614174000'
}));
```

**Server → All Conversation Participants:** `/topic/conversation/{conversationId}/typing`

```json
{
  "type": "TYPING_START",
  "conversationId": "123e4567-e89b-12d3-a456-426614174000",
  "senderId": "323e4567-e89b-12d3-a456-426614174002",
  "timestamp": "2024-01-15T14:30:10"
}
```

---

#### 5. Online/Offline Status

**User Online - Client → Server:** `/app/chat.online`

```javascript
stompClient.send('/app/chat.online', {}, JSON.stringify({}));
```

**User Offline - Client → Server:** `/app/chat.offline`

```javascript
stompClient.send('/app/chat.offline', {}, JSON.stringify({}));
```

**Server → All Users:** `/topic/users/status`

```json
{
  "type": "USER_ONLINE",
  "senderId": "323e4567-e89b-12d3-a456-426614174002",
  "timestamp": "2024-01-15T14:30:00"
}
```

---

### Complete Chat Client Example

```javascript
class ChatClient {
  constructor(jwtToken) {
    this.jwtToken = jwtToken;
    this.stompClient = null;
    this.connected = false;
  }

  connect() {
    const socket = new SockJS('http://localhost:8080/ws');
    this.stompClient = Stomp.over(socket);

    return new Promise((resolve, reject) => {
      this.stompClient.connect(
        { Authorization: `Bearer ${this.jwtToken}` },
        (frame) => {
          console.log('Connected:', frame);
          this.connected = true;
          this.setupSubscriptions();
          resolve();
        },
        (error) => {
          console.error('Connection error:', error);
          this.connected = false;
          reject(error);
        }
      );
    });
  }

  setupSubscriptions() {
    // Subscribe to incoming messages
    this.stompClient.subscribe('/user/queue/messages', (message) => {
      this.handleMessage(JSON.parse(message.body));
    });

    // Subscribe to notifications
    this.stompClient.subscribe('/user/queue/notifications', (notification) => {
      this.handleNotification(JSON.parse(notification.body));
    });

    // Subscribe to errors
    this.stompClient.subscribe('/user/queue/errors', (error) => {
      this.handleError(JSON.parse(error.body));
    });
  }

  subscribeToConversation(conversationId) {
    this.stompClient.subscribe(
      `/topic/conversation/${conversationId}/typing`,
      (typing) => {
        this.handleTyping(JSON.parse(typing.body));
      }
    );
  }

  sendMessage(conversationId, content) {
    if (!this.connected) {
      console.error('Not connected to WebSocket');
      return;
    }

    this.stompClient.send('/app/chat.send', {}, JSON.stringify({
      conversationId,
      content
    }));
  }

  markAsRead(conversationId, messageIds) {
    this.stompClient.send('/app/chat.markRead', {}, JSON.stringify({
      conversationId,
      messageIds
    }));
  }

  startTyping(conversationId) {
    this.stompClient.send('/app/chat.typing.start', {}, JSON.stringify({
      conversationId
    }));
  }

  stopTyping(conversationId) {
    this.stompClient.send('/app/chat.typing.stop', {}, JSON.stringify({
      conversationId
    }));
  }

  setOnline() {
    this.stompClient.send('/app/chat.online', {}, JSON.stringify({}));
  }

  setOffline() {
    this.stompClient.send('/app/chat.offline', {}, JSON.stringify({}));
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      this.setOffline();
      this.stompClient.disconnect();
      this.connected = false;
    }
  }

  handleMessage(message) {
    console.log('New message:', message);
    // Update UI with new message
    // Auto-acknowledge delivery
    this.stompClient.send('/app/chat.delivered', {}, JSON.stringify({
      messageId: message.messageId
    }));
  }

  handleNotification(notification) {
    console.log('Notification:', notification);
    // Handle delivery/read receipts
    if (notification.type === 'MESSAGE_DELIVERED') {
      // Update message status in UI
    } else if (notification.type === 'MESSAGE_READ') {
      // Update message status in UI
    }
  }

  handleTyping(typingData) {
    console.log('Typing indicator:', typingData);
    // Show/hide typing indicator in UI
  }

  handleError(error) {
    console.error('WebSocket error:', error);
    // Show error to user
  }
}

// Usage
const chatClient = new ChatClient('your-jwt-token');
chatClient.connect().then(() => {
  console.log('Chat client connected');
  chatClient.subscribeToConversation('conversation-uuid');
  chatClient.setOnline();
  chatClient.sendMessage('conversation-uuid', 'Hello!');
});
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

## Building and Running

### Run Locally with PostgreSQL (Default)

The service now uses PostgreSQL by default for persistence:

```bash
# 1. Ensure PostgreSQL is running and database is created (see Prerequisites above)

# 2. Start the service
cd backend/chat-service
mvn spring-boot:run

# Optional: Set database password if different from default 'postgres'
DB_PASSWORD=your_password mvn spring-boot:run
```

That's it! The service will start on **port 8083** with:
- PostgreSQL database (localhost:5432/dating_chat)
- Auto-schema creation (DDL-auto: update)
- WebSocket support for real-time messaging
- No Redis required for basic functionality (optional for session management)
- No RabbitMQ required for basic functionality (optional for distributed WebSocket)

**Access Points:**
- Service: http://localhost:8083/api/chat
- WebSocket: ws://localhost:8083/ws
- Health Check: http://localhost:8083/actuator/health

**Configuration:**
- Database: `jdbc:postgresql://localhost:5432/dating_chat`
- Username: `postgres` (default)
- Password: Set via `DB_PASSWORD` environment variable (default: `postgres`)

**Testing the API:**
You'll need:
1. JWT token from user-service (port 8081)
2. A valid match from match-service (port 8082) to create conversations

### Alternative: Run with H2 In-Memory Database

For quick testing without PostgreSQL, use the dev profile:

```bash
cd backend/chat-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Access Points (H2 mode):**
- H2 Console: http://localhost:8083/h2-console
  - JDBC URL: `jdbc:h2:mem:dating_chat_dev`
  - Username: `sa`
  - Password: (leave empty)

**Note:** H2 data is stored in memory and will be lost when the service stops. PostgreSQL is recommended for persistent chat history.

### Running with Production Profile (PostgreSQL + Redis + RabbitMQ)

For production-like setup with full features, you'll need Docker or manually installed services.

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 14+ (for prod profile)
- Redis 7+ (for prod profile)
- RabbitMQ 3.12+ (for prod profile, optional for distributed WebSocket)

### Local Development with PostgreSQL

1. **Start PostgreSQL:**
```bash
docker run -d \
  --name dating-chat-postgres \
  -e POSTGRES_DB=dating_chat \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:14
```

2. **Start Redis:**
```bash
docker run -d \
  --name dating-redis \
  -p 6379:6379 \
  redis:7-alpine
```

3. **Start RabbitMQ (optional):**
```bash
docker run -d \
  --name dating-rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

4. **Build and Run with Production Profile:**
```bash
cd backend/chat-service
mvn clean package -DskipTests

# Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Or run the JAR directly with prod profile
java -jar target/chat-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

The service will start on **port 8083**.

### Docker Build and Run

1. **Build Docker image:**
```bash
cd backend/chat-service
docker build -t dating-chat-service:latest .
```

2. **Run with Docker Compose:**
```yaml
version: '3.8'
services:
  chat-service:
    image: dating-chat-service:latest
    ports:
      - "8083:8083"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/dating_chat
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
      - SPRING_REDIS_HOST=redis
      - SPRING_RABBITMQ_HOST=rabbitmq
    depends_on:
      - postgres
      - redis
      - rabbitmq

  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: dating_chat
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - chat-postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "15672:15672"

volumes:
  chat-postgres-data:
```

### Configuration

Key configuration properties in `application.yml`:

```yaml
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/dating_chat

# JWT Secret (must match user-service)
jwt.secret=your-256-bit-secret-key-for-jwt-token-signing

# Feign Client URLs
feign.user-service.url=http://localhost:8081
feign.match-service.url=http://localhost:8082

# WebSocket Settings
websocket.allowed-origins=http://localhost:8080,http://localhost:3000
websocket.message-size-limit=8192

# Rate Limiting
rate-limit.messages-per-second=10
```

### Testing

**Run unit tests:**
```bash
mvn test
```

**Run integration tests:**
```bash
mvn verify
```

**Test WebSocket with wscat:**
```bash
npm install -g wscat
wscat -c ws://localhost:8083/ws -H "Authorization: Bearer <jwt-token>"
```

### Health Check

```bash
curl http://localhost:8083/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "rabbit": { "status": "UP" }
  }
}
```

## Security

### Authentication
- All endpoints require valid JWT token from user-service
- WebSocket connections must authenticate on CONNECT frame
- Tokens validated on every request

### Authorization
- Users can only access conversations they participate in
- Users can only delete their own messages
- Message content is not encrypted (future enhancement)

### Rate Limiting
- 10 messages per second per user (configurable)
- Prevents spam and abuse
- Returns 429 Too Many Requests when exceeded

### Input Validation
- Message content limited to 2000 characters
- SQL injection prevention via JPA
- XSS prevention via JSON serialization

## Monitoring and Metrics

Available via Spring Boot Actuator at `/actuator`:

- `/actuator/health` - Service health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics endpoint

**Key Metrics:**
- Active WebSocket connections
- Messages sent/received per minute
- Message delivery latency (p50, p95, p99)
- Database connection pool usage
- Redis cache hit/miss ratio

## Troubleshooting

### WebSocket Connection Fails
- Verify JWT token is valid and not expired
- Check CORS configuration in `websocket.allowed-origins`
- Ensure Authorization header format: `Bearer <token>`

### Messages Not Being Delivered
- Check if both users are connected to WebSocket
- Verify conversation exists and users are participants
- Check RabbitMQ is running (for distributed deployments)
- Review logs for errors: `tail -f logs/chat-service.log`

### Database Connection Issues
- Verify PostgreSQL is running and accessible
- Check database credentials in `application.yml`
- Ensure database `dating_chat` exists
- Check connection pool settings

### Redis Connection Issues
- Verify Redis is running: `redis-cli ping`
- Check Redis host/port configuration
- Review Redis logs for errors

## API Design Decisions

### Why WebSocket + REST?
- **REST:** Better for historical data, pagination, search
- **WebSocket:** Essential for real-time messaging, typing indicators
- Clients can choose based on use case

### Why STOMP Protocol?
- Industry standard for WebSocket messaging
- Built-in support for pub/sub patterns
- Easy to use with JavaScript clients
- SockJS fallback for older browsers

### Why Soft Delete Messages?
- Allows audit trail and data recovery
- Prevents accidental data loss
- Supports future "undo delete" feature
- Compliance with data retention policies

### Why PostgreSQL Over MongoDB?
- Strong consistency for message ordering
- ACID transactions for critical operations
- Complex queries for conversation filtering
- Mature ecosystem and tooling

## Future Enhancements

- **End-to-end encryption** - Encrypt message content
- **Image/media sharing** - Upload and share photos/videos
- **Voice messages** - Record and send audio
- **Video calling** - WebRTC integration
- **Message reactions** - React with emoji
- **Message editing** - Edit sent messages
- **Message forwarding** - Forward to other conversations
- **Full-text search** - Search message history
- **Message pinning** - Pin important messages
- **Message expiration** - Auto-delete after time
- **Group conversations** - Multi-user chat rooms
- **Push notifications** - Mobile push for offline users
- **Read receipts opt-out** - Privacy option
- **Message translation** - Auto-translate languages

## Contributing

See main project README for contribution guidelines.

## License

See main project LICENSE file.
