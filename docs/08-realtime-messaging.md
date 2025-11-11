# Real-time Messaging Implementation

## Overview

Real-time messaging is critical for dating apps. Users expect instant message delivery, typing indicators, and read receipts.

**Technology Stack:**
- **WebSocket**: Socket.IO for real-time bidirectional communication
- **MongoDB**: Message persistence
- **Redis**: Online presence, typing indicators, unread counts
- **Node.js**: WebSocket server

---

## Table of Contents

1. [Architecture](#architecture)
2. [WebSocket Server](#websocket-server)
3. [Message Flow](#message-flow)
4. [Presence System](#presence-system)
5. [Typing Indicators](#typing-indicators)
6. [Read Receipts](#read-receipts)
7. [Push Notifications](#push-notifications)
8. [Scaling](#scaling)

---

## Architecture

```
┌─────────────┐          ┌──────────────────┐
│ Mobile App  │◄────────►│  Load Balancer   │
└─────────────┘  WSS     └──────────────────┘
                                   │
                                   ▼
                  ┌────────────────────────────────┐
                  │   WebSocket Servers (N)        │
                  │   (Socket.IO)                   │
                  └────────────────────────────────┘
                          │         │        │
                          ▼         ▼        ▼
                  ┌──────────────────────────────┐
                  │   Redis Pub/Sub              │
                  │   (Cross-server messaging)   │
                  └──────────────────────────────┘
                          │
                          ▼
        ┌─────────────────┴──────────────────┐
        │                                    │
        ▼                                    ▼
 ┌─────────────┐                    ┌──────────────┐
 │  MongoDB    │                    │   Kafka      │
 │  (Messages) │                    │   (Events)   │
 └─────────────┘                    └──────────────┘
```

---

## WebSocket Server

### Server Setup

```typescript
// src/services/messaging/server.ts
import http from 'http';
import express from 'express';
import { Server as SocketIOServer } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';
import { redis } from '../../cache/client';
import { verifyToken } from '../auth/utils/jwt';
import { handleConnection } from './handlers/connectionHandler';

const app = express();
const httpServer = http.createServer(app);

// Initialize Socket.IO
const io = new SocketIOServer(httpServer, {
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
    credentials: true,
  },
  transports: ['websocket', 'polling'],
  pingTimeout: 60000,
  pingInterval: 25000,
});

// Redis adapter for multi-server support
const pubClient = redis.duplicate();
const subClient = redis.duplicate();

Promise.all([pubClient.connect(), subClient.connect()]).then(() => {
  io.adapter(createAdapter(pubClient, subClient));
  console.log('Socket.IO Redis adapter connected');
});

// Authentication middleware
io.use(async (socket, next) => {
  const token = socket.handshake.auth.token;

  if (!token) {
    return next(new Error('Authentication token required'));
  }

  try {
    const payload = await verifyToken(token);
    socket.data.userId = payload.userId;
    next();
  } catch (error) {
    next(new Error('Invalid or expired token'));
  }
});

// Connection handler
io.on('connection', (socket) => {
  handleConnection(socket, io);
});

// Start server
const PORT = process.env.WS_PORT || 3001;
httpServer.listen(PORT, () => {
  console.log(`WebSocket server running on port ${PORT}`);
});

export { io };
```

---

### Connection Handler

```typescript
// src/services/messaging/handlers/connectionHandler.ts
import { Socket } from 'socket.io';
import { Server as SocketIOServer } from 'socket.io';
import { redis } from '../../../cache/client';
import { handleSendMessage } from './messageHandler';
import { handleTyping } from './typingHandler';
import { db } from '../../../db/client';

export function handleConnection(socket: Socket, io: SocketIOServer) {
  const userId = socket.data.userId;

  console.log(`User ${userId} connected (socket: ${socket.id})`);

  // Join user to their personal room
  socket.join(`user:${userId}`);

  // Mark user as online
  redis.multi()
    .sadd('online:users', userId)
    .set(`socket:${userId}`, socket.id, 'EX', 3600)
    .exec();

  // Update last seen
  db.user.update({
    where: { id: userId },
    data: { lastSeen: new Date() },
  }).catch(console.error);

  // Broadcast online status to user's matches
  broadcastOnlineStatus(userId, true, io);

  // Event handlers
  socket.on('message:send', (data) => handleSendMessage(socket, io, data));
  socket.on('message:typing', (data) => handleTyping(socket, io, data));
  socket.on('message:read', (data) => handleMarkAsRead(socket, io, data));

  // Disconnection
  socket.on('disconnect', () => {
    console.log(`User ${userId} disconnected`);

    redis.multi()
      .srem('online:users', userId)
      .del(`socket:${userId}`)
      .exec();

    // Broadcast offline status
    broadcastOnlineStatus(userId, false, io);

    // Update last seen
    db.user.update({
      where: { id: userId },
      data: { lastSeen: new Date() },
    }).catch(console.error);
  });
}

async function broadcastOnlineStatus(
  userId: string,
  online: boolean,
  io: SocketIOServer
) {
  // Get user's matches
  const matches = await db.match.findMany({
    where: {
      OR: [
        { userAId: userId },
        { userBId: userId },
      ],
      unmatchedAt: null,
    },
    select: {
      userAId: true,
      userBId: true,
    },
  });

  // Notify each match
  for (const match of matches) {
    const otherUserId = match.userAId === userId ? match.userBId : match.userAId;

    io.to(`user:${otherUserId}`).emit('user:status', {
      userId,
      online,
      timestamp: new Date(),
    });
  }
}
```

---

## Message Flow

### Send Message Handler

```typescript
// src/services/messaging/handlers/messageHandler.ts
import { Socket } from 'socket.io';
import { Server as SocketIOServer } from 'socket.io';
import { Message } from '../models/Message';
import { db } from '../../../db/client';
import { redis } from '../../../cache/client';
import { kafka } from '../../../kafka/client';

interface SendMessageData {
  matchId: string;
  content: string;
  type?: 'text' | 'image' | 'voice' | 'video';
  mediaUrl?: string;
}

export async function handleSendMessage(
  socket: Socket,
  io: SocketIOServer,
  data: SendMessageData
) {
  const senderId = socket.data.userId;
  const { matchId, content, type = 'text', mediaUrl } = data;

  try {
    // 1. Verify match exists and user is part of it
    const match = await db.match.findFirst({
      where: {
        id: matchId,
        OR: [
          { userAId: senderId },
          { userBId: senderId },
        ],
        unmatchedAt: null,
      },
    });

    if (!match) {
      return socket.emit('error', {
        code: 'INVALID_MATCH',
        message: 'Match not found or you are not part of this match',
      });
    }

    const recipientId = match.userAId === senderId ? match.userBId : match.userAId;

    // 2. Save message to MongoDB
    const message = await Message.create({
      matchId,
      senderId,
      content,
      type,
      mediaUrl,
      sentAt: new Date(),
    });

    // 3. Check if recipient is online
    const recipientSocketId = await redis.get(`socket:${recipientId}`);

    let deliveredAt: Date | undefined;

    if (recipientSocketId) {
      // Recipient is online - send via WebSocket
      io.to(`user:${recipientId}`).emit('message:new', {
        id: message._id.toString(),
        matchId,
        senderId,
        content,
        type,
        mediaUrl,
        sentAt: message.sentAt,
      });

      deliveredAt = new Date();
      message.deliveredAt = deliveredAt;
      await message.save();

      // Acknowledge delivery to sender
      socket.emit('message:delivered', {
        messageId: message._id.toString(),
        deliveredAt,
      });
    } else {
      // Recipient is offline - queue push notification
      await kafka.publish('push_notifications', {
        userId: recipientId,
        type: 'new_message',
        data: {
          matchId,
          senderId,
          senderName: await getUserName(senderId),
          content: content.substring(0, 100),
          messageId: message._id.toString(),
        },
      });
    }

    // 4. Increment unread count for recipient
    await redis.incr(`unread:${matchId}:${recipientId}`);

    // 5. Update match's last message timestamp
    await db.match.update({
      where: { id: matchId },
      data: { lastMessageAt: new Date() },
    });

    // 6. Acknowledge to sender
    socket.emit('message:sent', {
      messageId: message._id.toString(),
      tempId: data.tempId, // Client-generated temp ID for optimistic updates
      sentAt: message.sentAt,
    });
  } catch (error) {
    console.error('Error sending message:', error);
    socket.emit('error', {
      code: 'MESSAGE_SEND_FAILED',
      message: 'Failed to send message',
    });
  }
}

async function getUserName(userId: string): Promise<string> {
  const user = await db.user.findUnique({
    where: { id: userId },
    select: { firstName: true },
  });

  return user?.firstName || 'Someone';
}
```

---

### Client Implementation (React Native)

```typescript
// mobile/src/services/websocket.ts
import io, { Socket } from 'socket.io-client';
import { store } from '../store';
import { addMessage, markMessageDelivered } from '../store/messagesSlice';

let socket: Socket | null = null;

export function connectWebSocket(token: string) {
  socket = io(process.env.WS_URL!, {
    auth: { token },
    transports: ['websocket'],
    reconnection: true,
    reconnectionDelay: 1000,
    reconnectionDelayMax: 5000,
    reconnectionAttempts: Infinity,
  });

  // Connection events
  socket.on('connect', () => {
    console.log('WebSocket connected');
  });

  socket.on('disconnect', () => {
    console.log('WebSocket disconnected');
  });

  socket.on('error', (error) => {
    console.error('WebSocket error:', error);
  });

  // Message events
  socket.on('message:new', (message) => {
    store.dispatch(addMessage(message));
  });

  socket.on('message:delivered', (data) => {
    store.dispatch(markMessageDelivered(data));
  });

  socket.on('message:read', (data) => {
    store.dispatch(markMessageRead(data));
  });

  socket.on('user:typing', (data) => {
    // Update UI to show typing indicator
  });

  socket.on('user:status', (data) => {
    // Update user online/offline status
  });

  return socket;
}

export function sendMessage(matchId: string, content: string, tempId: string) {
  if (!socket) {
    throw new Error('WebSocket not connected');
  }

  socket.emit('message:send', {
    matchId,
    content,
    type: 'text',
    tempId,
  });
}

export function sendTypingIndicator(matchId: string, isTyping: boolean) {
  if (!socket) return;

  socket.emit('message:typing', {
    matchId,
    isTyping,
  });
}

export function disconnectWebSocket() {
  if (socket) {
    socket.disconnect();
    socket = null;
  }
}
```

---

## Presence System

### Online/Offline Status

```typescript
// src/services/messaging/utils/presence.ts
import { redis } from '../../../cache/client';

export async function isUserOnline(userId: string): Promise<boolean> {
  const result = await redis.sismember('online:users', userId);
  return result === 1;
}

export async function getOnlineUsers(userIds: string[]): Promise<string[]> {
  if (userIds.length === 0) return [];

  const pipeline = redis.pipeline();
  userIds.forEach(id => pipeline.sismember('online:users', id));

  const results = await pipeline.exec();

  return userIds.filter((_, i) => results![i][1] === 1);
}

export async function getOnlineCount(): Promise<number> {
  return await redis.scard('online:users');
}
```

---

## Typing Indicators

### Typing Handler

```typescript
// src/services/messaging/handlers/typingHandler.ts
import { Socket } from 'socket.io';
import { Server as SocketIOServer } from 'socket.io';
import { redis } from '../../../cache/client';
import { db } from '../../../db/client';

interface TypingData {
  matchId: string;
  isTyping: boolean;
}

export async function handleTyping(
  socket: Socket,
  io: SocketIOServer,
  data: TypingData
) {
  const userId = socket.data.userId;
  const { matchId, isTyping } = data;

  try {
    // Get match to find recipient
    const match = await db.match.findUnique({
      where: { id: matchId },
      select: { userAId: true, userBId: true },
    });

    if (!match) return;

    const recipientId = match.userAId === userId ? match.userBId : match.userAId;

    if (isTyping) {
      // Set typing indicator with 5-second expiry
      await redis.setex(`typing:${matchId}:${userId}`, 5, '1');
    } else {
      // Clear typing indicator
      await redis.del(`typing:${matchId}:${userId}`);
    }

    // Send typing event to recipient
    io.to(`user:${recipientId}`).emit('user:typing', {
      matchId,
      userId,
      isTyping,
    });
  } catch (error) {
    console.error('Error handling typing indicator:', error);
  }
}
```

---

## Read Receipts

### Mark as Read Handler

```typescript
// src/services/messaging/handlers/readHandler.ts
import { Socket } from 'socket.io';
import { Server as SocketIOServer } from 'socket.io';
import { Message } from '../models/Message';
import { redis } from '../../../cache/client';
import { db } from '../../../db/client';

interface MarkAsReadData {
  matchId: string;
  messageIds: string[];
}

export async function handleMarkAsRead(
  socket: Socket,
  io: SocketIOServer,
  data: MarkAsReadData
) {
  const userId = socket.data.userId;
  const { matchId, messageIds } = data;

  try {
    // Verify match
    const match = await db.match.findFirst({
      where: {
        id: matchId,
        OR: [
          { userAId: userId },
          { userBId: userId },
        ],
      },
    });

    if (!match) return;

    const senderId = match.userAId === userId ? match.userBId : match.userAId;

    // Update messages in MongoDB
    const readAt = new Date();

    await Message.updateMany(
      {
        _id: { $in: messageIds },
        matchId,
        senderId, // Only mark messages FROM the other user
        readAt: null, // Not already read
      },
      {
        $set: { readAt },
      }
    );

    // Reset unread count
    await redis.set(`unread:${matchId}:${userId}`, 0);

    // Notify sender of read receipts
    io.to(`user:${senderId}`).emit('message:read', {
      matchId,
      messageIds,
      readAt,
      readBy: userId,
    });
  } catch (error) {
    console.error('Error marking messages as read:', error);
  }
}
```

---

## Push Notifications

### Notification Service

```typescript
// src/services/notifications/pushNotificationService.ts
import admin from 'firebase-admin';
import { kafka } from '../../kafka/client';
import { db } from '../../db/client';

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
  }),
});

// Kafka consumer for push notifications
kafka.subscribe('push_notifications', async (event) => {
  const { userId, type, data } = event;

  try {
    await sendPushNotification(userId, type, data);
  } catch (error) {
    console.error('Failed to send push notification:', error);
  }
});

async function sendPushNotification(
  userId: string,
  type: string,
  data: any
) {
  // Get user's device tokens
  const devices = await db.device.findMany({
    where: {
      userId,
      pushToken: { not: null },
    },
  });

  if (devices.length === 0) {
    console.log(`No devices found for user ${userId}`);
    return;
  }

  const tokens = devices.map(d => d.pushToken!);

  // Build notification
  const notification = buildNotification(type, data);

  // Send to all devices
  const response = await admin.messaging().sendMulticast({
    tokens,
    notification: {
      title: notification.title,
      body: notification.body,
    },
    data: {
      type,
      ...data,
    },
    apns: {
      payload: {
        aps: {
          sound: 'default',
          badge: await getUnreadBadgeCount(userId),
        },
      },
    },
  });

  console.log(`Push notification sent to ${userId}:`, response);

  // Handle failed tokens
  if (response.failureCount > 0) {
    const failedTokens = tokens.filter((_, i) => !response.responses[i].success);
    await removeInvalidTokens(userId, failedTokens);
  }
}

function buildNotification(type: string, data: any) {
  switch (type) {
    case 'new_message':
      return {
        title: data.senderName,
        body: data.content,
      };
    case 'new_match':
      return {
        title: "It's a Match!",
        body: `You and ${data.userName} liked each other`,
      };
    default:
      return {
        title: 'Dating App',
        body: 'You have a new notification',
      };
  }
}

async function getUnreadBadgeCount(userId: string): Promise<number> {
  // Get all matches for user
  const matches = await db.match.findMany({
    where: {
      OR: [
        { userAId: userId },
        { userBId: userId },
      ],
      unmatchedAt: null,
    },
    select: { id: true },
  });

  // Sum unread counts from Redis
  let totalUnread = 0;

  for (const match of matches) {
    const unread = await redis.get(`unread:${match.id}:${userId}`);
    totalUnread += parseInt(unread || '0');
  }

  return totalUnread;
}

async function removeInvalidTokens(userId: string, tokens: string[]) {
  await db.device.deleteMany({
    where: {
      userId,
      pushToken: { in: tokens },
    },
  });
}
```

---

## Scaling

### Multi-Server Setup with Redis Pub/Sub

When scaling to multiple WebSocket servers, use Redis adapter for cross-server communication.

```typescript
// Multiple WebSocket server instances
// Server 1: User A connects
// Server 2: User B connects
// When A sends message to B, Redis Pub/Sub ensures B receives it

// Socket.IO automatically handles this with Redis adapter
import { createAdapter } from '@socket.io/redis-adapter';

io.adapter(createAdapter(pubClient, subClient));

// Now messages are automatically distributed across all servers
```

---

### Load Balancing

```nginx
# nginx.conf
upstream websocket_servers {
    ip_hash; # Sticky sessions - important for WebSocket

    server ws-server-1:3001;
    server ws-server-2:3001;
    server ws-server-3:3001;
}

server {
    listen 80;
    server_name ws.datingapp.com;

    location / {
        proxy_pass http://websocket_servers;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # Timeouts
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
    }
}
```

---

### Horizontal Scaling

```yaml
# kubernetes/websocket-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: websocket-server
spec:
  replicas: 5 # Scale horizontally
  selector:
    matchLabels:
      app: websocket-server
  template:
    metadata:
      labels:
        app: websocket-server
    spec:
      containers:
      - name: websocket-server
        image: datingapp/websocket-server:latest
        ports:
        - containerPort: 3001
        env:
        - name: REDIS_HOST
          value: redis-cluster
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: mongodb-uri
        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 1Gi
---
apiVersion: v1
kind: Service
metadata:
  name: websocket-service
spec:
  selector:
    app: websocket-server
  ports:
  - protocol: TCP
    port: 80
    targetPort: 3001
  sessionAffinity: ClientIP # Sticky sessions
```

---

## Performance Monitoring

### Metrics to Track

```typescript
// src/services/messaging/metrics.ts
import { io } from './server';
import { redis } from '../../cache/client';

export async function getWebSocketMetrics() {
  const [
    connectedUsers,
    onlineUsers,
    totalRooms,
  ] = await Promise.all([
    io.sockets.sockets.size,
    redis.scard('online:users'),
    io.sockets.adapter.rooms.size,
  ]);

  return {
    connectedSockets: connectedUsers,
    onlineUsers,
    rooms: totalRooms,
    timestamp: new Date(),
  };
}

// Emit metrics every 30 seconds
setInterval(async () => {
  const metrics = await getWebSocketMetrics();
  console.log('WebSocket Metrics:', metrics);

  // Send to monitoring service (DataDog, CloudWatch, etc.)
}, 30000);
```

---

## Next Steps

- [Geolocation Service Implementation](09-geolocation-service.md)
- [Infrastructure & Deployment](10-infrastructure.md)
- [Microservices Implementation](11-microservices.md)
