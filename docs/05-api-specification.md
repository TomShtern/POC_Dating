# API Specification

## Overview

RESTful API design for dating app backend services. All endpoints use JSON for request/response bodies.

**Base URL**: `https://api.datingapp.com/v1`

**Authentication**: Bearer token (JWT) in Authorization header

```
Authorization: Bearer <jwt_token>
```

---

## Table of Contents

1. [Authentication Service](#authentication-service)
2. [User Service](#user-service)
3. [Match Service](#match-service)
4. [Messaging Service](#messaging-service)
5. [Geolocation Service](#geolocation-service)
6. [Media Service](#media-service)
7. [WebSocket Events](#websocket-events)

---

## Authentication Service

### POST /auth/register

Register a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "phone": "+1234567890",
  "firstName": "John",
  "dateOfBirth": "1995-03-15",
  "gender": "male"
}
```

**Response:** `201 Created`
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "verified": false,
    "createdAt": "2024-11-06T10:30:00Z"
  },
  "tokens": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440001",
    "expiresIn": 900
  }
}
```

**Implementation:**
```typescript
// src/services/auth/controllers/authController.ts
import { Request, Response } from 'express';
import { hash } from 'bcryptjs';
import { v4 as uuidv4 } from 'uuid';
import { db } from '../../../db/client';
import { generateTokens } from '../utils/jwt';
import { validateRegistration } from '../validators/authValidator';

export async function register(req: Request, res: Response) {
  // Validate input
  const { error, value } = validateRegistration(req.body);
  if (error) {
    return res.status(400).json({ error: error.details[0].message });
  }

  const { email, password, phone, firstName, dateOfBirth, gender } = value;

  // Check if user already exists
  const existingUser = await db.user.findUnique({ where: { email } });
  if (existingUser) {
    return res.status(409).json({ error: 'Email already registered' });
  }

  // Hash password
  const passwordHash = await hash(password, 12);

  // Create user
  const user = await db.user.create({
    data: {
      id: uuidv4(),
      email,
      passwordHash,
      phone,
      firstName,
      dateOfBirth: new Date(dateOfBirth),
      gender,
      verified: false,
    },
  });

  // Generate tokens
  const tokens = await generateTokens(user.id);

  // Send verification email (async, don't wait)
  sendVerificationEmail(user.email, user.id).catch(console.error);

  res.status(201).json({
    user: {
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      verified: user.verified,
      createdAt: user.createdAt,
    },
    tokens,
  });
}
```

---

### POST /auth/login

Login with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response:** `200 OK`
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "verified": true
  },
  "tokens": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440002",
    "expiresIn": 900
  }
}
```

**Implementation:**
```typescript
// src/services/auth/controllers/authController.ts
import { compare } from 'bcryptjs';

export async function login(req: Request, res: Response) {
  const { email, password } = req.body;

  // Find user
  const user = await db.user.findUnique({
    where: { email },
    select: {
      id: true,
      email: true,
      firstName: true,
      passwordHash: true,
      verified: true,
    },
  });

  if (!user) {
    return res.status(401).json({ error: 'Invalid credentials' });
  }

  // Verify password
  const isValid = await compare(password, user.passwordHash);
  if (!isValid) {
    return res.status(401).json({ error: 'Invalid credentials' });
  }

  // Generate tokens
  const tokens = await generateTokens(user.id);

  // Update last login
  await db.user.update({
    where: { id: user.id },
    data: { lastSeen: new Date() },
  });

  res.json({
    user: {
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      verified: user.verified,
    },
    tokens,
  });
}
```

---

### POST /auth/refresh

Refresh access token using refresh token.

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440003",
  "expiresIn": 900
}
```

**Implementation:**
```typescript
export async function refreshAccessToken(req: Request, res: Response) {
  const { refreshToken } = req.body;

  // Validate refresh token
  const storedToken = await redis.get(`refresh:${refreshToken}`);
  if (!storedToken) {
    return res.status(401).json({ error: 'Invalid refresh token' });
  }

  const userId = JSON.parse(storedToken).userId;

  // Delete old refresh token
  await redis.del(`refresh:${refreshToken}`);

  // Generate new tokens
  const tokens = await generateTokens(userId);

  res.json(tokens);
}
```

---

### POST /auth/oauth/google

Login with Google OAuth.

**Request:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlOWdkazcifQ..."
}
```

**Response:** `200 OK` (same as login)

**Implementation:**
```typescript
import { OAuth2Client } from 'google-auth-library';

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

export async function googleOAuth(req: Request, res: Response) {
  const { idToken } = req.body;

  // Verify Google token
  const ticket = await googleClient.verifyIdToken({
    idToken,
    audience: process.env.GOOGLE_CLIENT_ID,
  });

  const payload = ticket.getPayload();
  if (!payload) {
    return res.status(401).json({ error: 'Invalid Google token' });
  }

  const { sub: googleId, email, given_name, picture } = payload;

  // Find or create user
  let user = await db.user.findUnique({ where: { email } });

  if (!user) {
    user = await db.user.create({
      data: {
        id: uuidv4(),
        email,
        firstName: given_name,
        googleId,
        verified: true, // Google emails are pre-verified
      },
    });

    // Download and save profile picture
    if (picture) {
      saveProfilePictureFromUrl(user.id, picture).catch(console.error);
    }
  }

  const tokens = await generateTokens(user.id);

  res.json({
    user: {
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      verified: user.verified,
    },
    tokens,
  });
}
```

---

## User Service

### GET /users/me

Get current user's profile.

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "dateOfBirth": "1995-03-15",
  "gender": "male",
  "bio": "Love hiking and coffee",
  "location": {
    "lat": 40.7128,
    "lng": -74.0060,
    "city": "New York",
    "updatedAt": "2024-11-06T10:30:00Z"
  },
  "photos": [
    {
      "id": "photo-1",
      "url": "https://cdn.datingapp.com/photos/user-id/photo-1.jpg",
      "order": 0,
      "isPrimary": true
    }
  ],
  "preferences": {
    "minAge": 25,
    "maxAge": 35,
    "maxDistance": 50,
    "interestedIn": "female"
  },
  "verified": true,
  "createdAt": "2024-10-01T12:00:00Z"
}
```

**Implementation:**
```typescript
// src/services/user/controllers/userController.ts
import { Request, Response } from 'express';
import { db } from '../../../db/client';
import { redis } from '../../../cache/client';

export async function getCurrentUser(req: Request, res: Response) {
  const userId = req.user.id; // From JWT middleware

  // Try cache first
  const cached = await redis.get(`user:profile:${userId}`);
  if (cached) {
    return res.json(JSON.parse(cached));
  }

  // Fetch from database
  const user = await db.user.findUnique({
    where: { id: userId },
    include: {
      photos: {
        orderBy: { order: 'asc' },
      },
      preferences: true,
    },
  });

  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  // Get latest location from Redis (more current than DB)
  const location = await redis.geopos('users:locations', userId);

  const response = {
    id: user.id,
    email: user.email,
    firstName: user.firstName,
    dateOfBirth: user.dateOfBirth,
    gender: user.gender,
    bio: user.bio,
    location: location ? {
      lat: parseFloat(location[0][1]),
      lng: parseFloat(location[0][0]),
      updatedAt: new Date(),
    } : null,
    photos: user.photos.map(p => ({
      id: p.id,
      url: p.url,
      order: p.order,
      isPrimary: p.isPrimary,
    })),
    preferences: user.preferences,
    verified: user.verified,
    createdAt: user.createdAt,
  };

  // Cache for 1 hour
  await redis.setex(
    `user:profile:${userId}`,
    3600,
    JSON.stringify(response)
  );

  res.json(response);
}
```

---

### PUT /users/me

Update current user's profile.

**Request:**
```json
{
  "firstName": "John",
  "bio": "Updated bio text",
  "preferences": {
    "minAge": 25,
    "maxAge": 35,
    "maxDistance": 50,
    "interestedIn": "female"
  }
}
```

**Response:** `200 OK` (updated user object)

**Implementation:**
```typescript
export async function updateProfile(req: Request, res: Response) {
  const userId = req.user.id;
  const { firstName, bio, preferences } = req.body;

  // Update user
  const user = await db.user.update({
    where: { id: userId },
    data: {
      firstName,
      bio,
      preferences: preferences ? {
        upsert: {
          create: preferences,
          update: preferences,
        },
      } : undefined,
    },
    include: {
      photos: true,
      preferences: true,
    },
  });

  // Invalidate cache
  await redis.del(`user:profile:${userId}`);
  await redis.del(`match:candidates:${userId}`);

  res.json(user);
}
```

---

### POST /users/me/photos

Upload a new photo.

**Request:** `multipart/form-data`
```
photo: <binary file>
order: 0
isPrimary: true
```

**Response:** `201 Created`
```json
{
  "id": "photo-123",
  "url": "https://cdn.datingapp.com/photos/user-id/photo-123.jpg",
  "thumbnailUrl": "https://cdn.datingapp.com/photos/user-id/photo-123-thumb.jpg",
  "order": 0,
  "isPrimary": true
}
```

**Implementation:**
```typescript
import multer from 'multer';
import sharp from 'sharp';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';

const s3 = new S3Client({ region: process.env.AWS_REGION });
const upload = multer({ storage: multer.memoryStorage() });

export const uploadPhoto = [
  upload.single('photo'),
  async (req: Request, res: Response) => {
    const userId = req.user.id;
    const file = req.file;

    if (!file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }

    const photoId = uuidv4();
    const baseKey = `photos/${userId}/${photoId}`;

    // Process image - original
    const originalBuffer = await sharp(file.buffer)
      .resize(1080, 1080, { fit: 'cover' })
      .jpeg({ quality: 90 })
      .toBuffer();

    // Process image - thumbnail
    const thumbnailBuffer = await sharp(file.buffer)
      .resize(300, 300, { fit: 'cover' })
      .jpeg({ quality: 80 })
      .toBuffer();

    // Upload to S3
    await Promise.all([
      s3.send(new PutObjectCommand({
        Bucket: process.env.S3_BUCKET,
        Key: `${baseKey}.jpg`,
        Body: originalBuffer,
        ContentType: 'image/jpeg',
      })),
      s3.send(new PutObjectCommand({
        Bucket: process.env.S3_BUCKET,
        Key: `${baseKey}-thumb.jpg`,
        Body: thumbnailBuffer,
        ContentType: 'image/jpeg',
      })),
    ]);

    // Save to database
    const photo = await db.photo.create({
      data: {
        id: photoId,
        userId,
        url: `https://cdn.datingapp.com/${baseKey}.jpg`,
        thumbnailUrl: `https://cdn.datingapp.com/${baseKey}-thumb.jpg`,
        order: parseInt(req.body.order) || 0,
        isPrimary: req.body.isPrimary === 'true',
      },
    });

    // Invalidate cache
    await redis.del(`user:profile:${userId}`);

    res.status(201).json(photo);
  }
];
```

---

### PUT /users/me/location

Update current location.

**Request:**
```json
{
  "lat": 40.7128,
  "lng": -74.0060
}
```

**Response:** `200 OK`
```json
{
  "lat": 40.7128,
  "lng": -74.0060,
  "updatedAt": "2024-11-06T10:30:00Z"
}
```

**Implementation:**
```typescript
export async function updateLocation(req: Request, res: Response) {
  const userId = req.user.id;
  const { lat, lng } = req.body;

  // Validate coordinates
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
    return res.status(400).json({ error: 'Invalid coordinates' });
  }

  // Store in Redis (primary location store)
  await redis.geoadd('users:locations', lng, lat, userId);

  // Update database (background, less frequently)
  db.user.update({
    where: { id: userId },
    data: {
      location: {
        type: 'Point',
        coordinates: [lng, lat],
      },
    },
  }).catch(console.error);

  // Invalidate match candidates (location changed)
  await redis.del(`match:candidates:${userId}`);

  res.json({
    lat,
    lng,
    updatedAt: new Date(),
  });
}
```

---

## Match Service

### GET /matches/candidates

Get potential matches (discovery feed).

**Query Parameters:**
- `limit` (default: 50)
- `offset` (default: 0)

**Response:** `200 OK`
```json
{
  "candidates": [
    {
      "id": "user-2",
      "firstName": "Jane",
      "age": 28,
      "bio": "Love outdoor adventures",
      "photos": [
        {
          "url": "https://cdn.datingapp.com/photos/user-2/photo-1.jpg",
          "order": 0
        }
      ],
      "distance": 3.5,
      "compatibility": 85
    }
  ],
  "hasMore": true
}
```

**Implementation:**
```typescript
// src/services/match/controllers/matchController.ts
import { Request, Response } from 'express';
import { db } from '../../../db/client';
import { redis } from '../../../cache/client';
import { calculateCompatibility } from '../utils/matchingAlgorithm';

export async function getCandidates(req: Request, res: Response) {
  const userId = req.user.id;
  const limit = parseInt(req.query.limit as string) || 50;
  const offset = parseInt(req.query.offset as string) || 0;

  // Check cache
  const cacheKey = `match:candidates:${userId}:${offset}:${limit}`;
  const cached = await redis.get(cacheKey);
  if (cached) {
    return res.json(JSON.parse(cached));
  }

  // Get user preferences
  const user = await db.user.findUnique({
    where: { id: userId },
    include: { preferences: true },
  });

  if (!user || !user.preferences) {
    return res.status(400).json({ error: 'Please set your preferences first' });
  }

  // Get user location
  const userLocation = await redis.geopos('users:locations', userId);
  if (!userLocation || !userLocation[0]) {
    return res.status(400).json({ error: 'Location not set' });
  }

  const [userLng, userLat] = userLocation[0];

  // Get users within distance
  const nearbyUsers = await redis.georadius(
    'users:locations',
    parseFloat(userLng),
    parseFloat(userLat),
    user.preferences.maxDistance,
    'km',
    'WITHDIST'
  );

  // Get users already swiped on
  const swipedUserIds = await db.swipe.findMany({
    where: { userId },
    select: { targetUserId: true },
  }).then(swipes => swipes.map(s => s.targetUserId));

  // Filter candidates
  const candidateIds = nearbyUsers
    .map(([id, distance]) => ({ id, distance: parseFloat(distance) }))
    .filter(({ id }) => id !== userId && !swipedUserIds.includes(id))
    .slice(offset, offset + limit);

  if (candidateIds.length === 0) {
    return res.json({ candidates: [], hasMore: false });
  }

  // Fetch candidate profiles
  const candidates = await db.user.findMany({
    where: {
      id: { in: candidateIds.map(c => c.id) },
      gender: user.preferences.interestedIn,
      dateOfBirth: {
        gte: new Date(new Date().getFullYear() - user.preferences.maxAge, 0, 1),
        lte: new Date(new Date().getFullYear() - user.preferences.minAge, 11, 31),
      },
    },
    include: {
      photos: {
        orderBy: { order: 'asc' },
        take: 5,
      },
    },
  });

  // Calculate compatibility scores
  const enrichedCandidates = await Promise.all(
    candidates.map(async (candidate) => {
      const distance = candidateIds.find(c => c.id === candidate.id)?.distance || 0;
      const compatibility = await calculateCompatibility(user, candidate);

      return {
        id: candidate.id,
        firstName: candidate.firstName,
        age: calculateAge(candidate.dateOfBirth),
        bio: candidate.bio,
        photos: candidate.photos.map(p => ({
          url: p.url,
          order: p.order,
        })),
        distance: Math.round(distance * 10) / 10,
        compatibility,
      };
    })
  );

  // Sort by compatibility
  enrichedCandidates.sort((a, b) => b.compatibility - a.compatibility);

  const response = {
    candidates: enrichedCandidates,
    hasMore: nearbyUsers.length > offset + limit,
  };

  // Cache for 5 minutes
  await redis.setex(cacheKey, 300, JSON.stringify(response));

  res.json(response);
}

function calculateAge(dateOfBirth: Date): number {
  const today = new Date();
  const birthDate = new Date(dateOfBirth);
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  return age;
}
```

---

### POST /matches/swipe

Swipe on a user (like/dislike).

**Request:**
```json
{
  "targetUserId": "user-2",
  "action": "like"
}
```

**Response:** `200 OK`
```json
{
  "matched": true,
  "matchId": "match-123"
}
```

Or if no match:
```json
{
  "matched": false
}
```

**Implementation:**
```typescript
export async function swipe(req: Request, res: Response) {
  const userId = req.user.id;
  const { targetUserId, action } = req.body;

  if (!['like', 'dislike'].includes(action)) {
    return res.status(400).json({ error: 'Invalid action' });
  }

  // Create swipe record
  const swipe = await db.swipe.create({
    data: {
      id: uuidv4(),
      userId,
      targetUserId,
      action,
    },
  });

  // If like, check for mutual match
  if (action === 'like') {
    const reciprocalSwipe = await db.swipe.findFirst({
      where: {
        userId: targetUserId,
        targetUserId: userId,
        action: 'like',
      },
    });

    if (reciprocalSwipe) {
      // It's a match!
      const matchId = uuidv4();

      await db.match.create({
        data: {
          id: matchId,
          userAId: userId < targetUserId ? userId : targetUserId,
          userBId: userId < targetUserId ? targetUserId : userId,
        },
      });

      // Publish match event to Kafka
      await kafka.publish('matches', {
        matchId,
        userAId: userId,
        userBId: targetUserId,
        timestamp: new Date(),
      });

      return res.json({
        matched: true,
        matchId,
      });
    }
  }

  // Invalidate cache
  await redis.del(`match:candidates:${userId}:*`);

  res.json({ matched: false });
}
```

---

### GET /matches

Get all matches for current user.

**Response:** `200 OK`
```json
{
  "matches": [
    {
      "id": "match-123",
      "user": {
        "id": "user-2",
        "firstName": "Jane",
        "age": 28,
        "photo": "https://cdn.datingapp.com/photos/user-2/photo-1.jpg"
      },
      "matchedAt": "2024-11-06T10:30:00Z",
      "lastMessage": {
        "content": "Hey there!",
        "sentAt": "2024-11-06T10:35:00Z",
        "senderId": "user-2"
      },
      "unreadCount": 2
    }
  ]
}
```

**Implementation:**
```typescript
export async function getMatches(req: Request, res: Response) {
  const userId = req.user.id;

  const matches = await db.match.findMany({
    where: {
      OR: [
        { userAId: userId },
        { userBId: userId },
      ],
      unmatchedAt: null,
    },
    include: {
      userA: {
        include: {
          photos: {
            where: { isPrimary: true },
            take: 1,
          },
        },
      },
      userB: {
        include: {
          photos: {
            where: { isPrimary: true },
            take: 1,
          },
        },
      },
    },
    orderBy: {
      matchedAt: 'desc',
    },
  });

  // Get last message for each match
  const enrichedMatches = await Promise.all(
    matches.map(async (match) => {
      const otherUser = match.userAId === userId ? match.userB : match.userA;

      // Get last message from MongoDB
      const lastMessage = await Message.findOne({
        matchId: match.id,
      }).sort({ sentAt: -1 }).limit(1);

      // Get unread count from Redis
      const unreadCount = await redis.get(`unread:${match.id}:${userId}`) || 0;

      return {
        id: match.id,
        user: {
          id: otherUser.id,
          firstName: otherUser.firstName,
          age: calculateAge(otherUser.dateOfBirth),
          photo: otherUser.photos[0]?.url,
        },
        matchedAt: match.matchedAt,
        lastMessage: lastMessage ? {
          content: lastMessage.content,
          sentAt: lastMessage.sentAt,
          senderId: lastMessage.senderId,
        } : null,
        unreadCount: parseInt(unreadCount),
      };
    })
  );

  res.json({ matches: enrichedMatches });
}
```

---

## Messaging Service

### GET /messages/:matchId

Get messages for a match.

**Query Parameters:**
- `limit` (default: 50)
- `before` (cursor for pagination)

**Response:** `200 OK`
```json
{
  "messages": [
    {
      "id": "msg-1",
      "matchId": "match-123",
      "senderId": "user-1",
      "content": "Hey! How are you?",
      "type": "text",
      "sentAt": "2024-11-06T10:30:00Z",
      "deliveredAt": "2024-11-06T10:30:01Z",
      "readAt": "2024-11-06T10:30:05Z"
    }
  ],
  "hasMore": false
}
```

**Implementation:**
```typescript
// src/services/messaging/controllers/messageController.ts
import { Request, Response } from 'express';
import { Message } from '../models/Message';

export async function getMessages(req: Request, res: Response) {
  const userId = req.user.id;
  const { matchId } = req.params;
  const limit = parseInt(req.query.limit as string) || 50;
  const before = req.query.before as string;

  // Verify user is part of this match
  const match = await db.match.findFirst({
    where: {
      id: matchId,
      OR: [
        { userAId: userId },
        { userBId: userId },
      ],
    },
  });

  if (!match) {
    return res.status(404).json({ error: 'Match not found' });
  }

  // Build query
  const query: any = { matchId };
  if (before) {
    query.sentAt = { $lt: new Date(before) };
  }

  // Fetch messages from MongoDB
  const messages = await Message.find(query)
    .sort({ sentAt: -1 })
    .limit(limit + 1);

  const hasMore = messages.length > limit;
  if (hasMore) {
    messages.pop();
  }

  // Mark messages as read
  const unreadMessageIds = messages
    .filter(m => m.senderId !== userId && !m.readAt)
    .map(m => m._id);

  if (unreadMessageIds.length > 0) {
    await Message.updateMany(
      { _id: { $in: unreadMessageIds } },
      { $set: { readAt: new Date() } }
    );

    // Update unread count in Redis
    await redis.decr(`unread:${matchId}:${userId}`, unreadMessageIds.length);

    // Emit read receipts via WebSocket
    for (const msg of messages.filter(m => unreadMessageIds.includes(m._id))) {
      io.to(`user:${msg.senderId}`).emit('message:read', {
        messageId: msg._id,
        readAt: new Date(),
      });
    }
  }

  res.json({
    messages: messages.reverse(),
    hasMore,
  });
}
```

---

### Message Schema (MongoDB)
```typescript
// src/services/messaging/models/Message.ts
import mongoose from 'mongoose';

const messageSchema = new mongoose.Schema({
  matchId: { type: String, required: true, index: true },
  senderId: { type: String, required: true },
  content: { type: String, required: true },
  type: { type: String, enum: ['text', 'image', 'voice'], default: 'text' },
  mediaUrl: String,
  sentAt: { type: Date, default: Date.now, index: true },
  deliveredAt: Date,
  readAt: Date,
  deletedBy: [String],
});

messageSchema.index({ matchId: 1, sentAt: -1 });

export const Message = mongoose.model('Message', messageSchema);
```

---

## Geolocation Service

### POST /geo/nearby

Find nearby users (used internally by match service).

**Request:**
```json
{
  "lat": 40.7128,
  "lng": -74.0060,
  "radius": 50,
  "unit": "km",
  "limit": 100
}
```

**Response:** `200 OK`
```json
{
  "users": [
    {
      "userId": "user-2",
      "distance": 3.5,
      "lat": 40.7289,
      "lng": -73.9944
    }
  ]
}
```

**Implementation:**
```typescript
// src/services/geo/controllers/geoController.ts
export async function findNearby(req: Request, res: Response) {
  const { lat, lng, radius, unit, limit } = req.body;

  const results = await redis.georadius(
    'users:locations',
    lng,
    lat,
    radius,
    unit || 'km',
    'WITHDIST',
    'WITHCOORD',
    'COUNT',
    limit || 100,
    'ASC'
  );

  const users = results.map(([userId, distance, [lng, lat]]) => ({
    userId,
    distance: parseFloat(distance),
    lat: parseFloat(lat),
    lng: parseFloat(lng),
  }));

  res.json({ users });
}
```

---

## WebSocket Events

### Connection

**Client connects:**
```javascript
const socket = io('wss://api.datingapp.com/ws', {
  auth: {
    token: accessToken
  }
});
```

**Server authenticates:**
```typescript
// src/services/messaging/websocket/server.ts
import { Server } from 'socket.io';
import { verifyToken } from '../utils/jwt';

export function initializeWebSocket(server: any) {
  const io = new Server(server, {
    cors: {
      origin: process.env.ALLOWED_ORIGINS?.split(','),
      credentials: true,
    },
  });

  // Authentication middleware
  io.use(async (socket, next) => {
    const token = socket.handshake.auth.token;
    try {
      const payload = await verifyToken(token);
      socket.data.userId = payload.userId;
      next();
    } catch (error) {
      next(new Error('Authentication failed'));
    }
  });

  io.on('connection', (socket) => {
    const userId = socket.data.userId;
    console.log(`User ${userId} connected`);

    // Join user's personal room
    socket.join(`user:${userId}`);

    // Mark user as online
    redis.sadd('online:users', userId);
    redis.set(`socket:${userId}`, socket.id, 'EX', 3600);

    // Handle disconnection
    socket.on('disconnect', () => {
      redis.srem('online:users', userId);
      redis.del(`socket:${userId}`);
    });

    // Message events
    socket.on('message:send', handleSendMessage(socket, io));
    socket.on('message:typing', handleTyping(socket, io));
  });

  return io;
}
```

### Send Message

**Client emits:**
```javascript
socket.emit('message:send', {
  matchId: 'match-123',
  content: 'Hello!',
  type: 'text'
});
```

**Server handles:**
```typescript
function handleSendMessage(socket: Socket, io: Server) {
  return async (data: { matchId: string; content: string; type: string }) => {
    const userId = socket.data.userId;
    const { matchId, content, type } = data;

    // Verify match exists
    const match = await db.match.findFirst({
      where: {
        id: matchId,
        OR: [
          { userAId: userId },
          { userBId: userId },
        ],
      },
    });

    if (!match) {
      return socket.emit('error', { message: 'Invalid match' });
    }

    const recipientId = match.userAId === userId ? match.userBId : match.userAId;

    // Create message
    const message = await Message.create({
      matchId,
      senderId: userId,
      content,
      type,
      sentAt: new Date(),
    });

    // Send to recipient if online
    const recipientSocketId = await redis.get(`socket:${recipientId}`);
    if (recipientSocketId) {
      io.to(`user:${recipientId}`).emit('message:new', {
        id: message._id,
        matchId,
        senderId: userId,
        content,
        type,
        sentAt: message.sentAt,
      });

      // Update delivered timestamp
      message.deliveredAt = new Date();
      await message.save();

      socket.emit('message:delivered', {
        messageId: message._id,
        deliveredAt: message.deliveredAt,
      });
    } else {
      // Send push notification
      await kafka.publish('push_notifications', {
        userId: recipientId,
        type: 'new_message',
        data: {
          matchId,
          senderId: userId,
          content: content.substring(0, 100),
        },
      });
    }

    // Increment unread count
    await redis.incr(`unread:${matchId}:${recipientId}`);

    // Acknowledge to sender
    socket.emit('message:sent', {
      messageId: message._id,
      sentAt: message.sentAt,
    });
  };
}
```

### Typing Indicator

**Client emits:**
```javascript
socket.emit('message:typing', {
  matchId: 'match-123',
  isTyping: true
});
```

**Server handles:**
```typescript
function handleTyping(socket: Socket, io: Server) {
  return async (data: { matchId: string; isTyping: boolean }) => {
    const userId = socket.data.userId;
    const { matchId, isTyping } = data;

    // Get match to find recipient
    const match = await db.match.findUnique({ where: { id: matchId } });
    if (!match) return;

    const recipientId = match.userAId === userId ? match.userBId : match.userAId;

    // Forward to recipient
    io.to(`user:${recipientId}`).emit('user:typing', {
      matchId,
      userId,
      isTyping,
    });
  };
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "error": "Error message",
  "code": "ERROR_CODE",
  "details": {}
}
```

**Common Status Codes:**
- `400` - Bad Request (validation error)
- `401` - Unauthorized (invalid/missing token)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found
- `409` - Conflict (e.g., email already exists)
- `422` - Unprocessable Entity
- `429` - Too Many Requests (rate limit)
- `500` - Internal Server Error

---

## Rate Limiting

All endpoints are rate-limited:

```typescript
// src/middleware/rateLimiter.ts
import rateLimit from 'express-rate-limit';
import RedisStore from 'rate-limit-redis';
import { redis } from '../cache/client';

export const apiLimiter = rateLimit({
  store: new RedisStore({
    client: redis,
    prefix: 'rl:',
  }),
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // 100 requests per window
  message: 'Too many requests, please try again later',
});

export const authLimiter = rateLimit({
  store: new RedisStore({
    client: redis,
    prefix: 'rl:auth:',
  }),
  windowMs: 15 * 60 * 1000,
  max: 5, // 5 login attempts per 15 minutes
  skipSuccessfulRequests: true,
});
```

---

## Next Steps

See implementation guides:
- [Database Schemas](05-database-schemas.md)
- [Matching Algorithm](06-matching-algorithm.md)
- [Real-time Messaging](07-realtime-messaging.md)
- [Infrastructure & Deployment](08-infrastructure.md)
