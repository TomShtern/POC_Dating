# Security Implementation

## Table of Contents
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Rate Limiting](#rate-limiting)
- [Password Hashing](#password-hashing)
- [OAuth 2.0 Social Login](#oauth-20-social-login)
- [Input Validation & Sanitization](#input-validation--sanitization)
- [Security Headers](#security-headers)
- [Encryption](#encryption)

---

## Authentication

### JWT with Refresh Token Strategy

**Why This Approach:**
- Short-lived access tokens (5-15 minutes) minimize risk if compromised
- Long-lived refresh tokens (7-30 days) reduce re-authentication friction
- Refresh tokens stored in httpOnly cookies prevent XSS attacks
- Token rotation on refresh prevents replay attacks

**Token Configuration:**

```typescript
// packages/auth/src/config/jwt.config.ts
export const JWT_CONFIG = {
  access: {
    secret: process.env.JWT_ACCESS_SECRET!,
    expiresIn: '15m', // 15 minutes
    algorithm: 'HS256' as const,
  },
  refresh: {
    secret: process.env.JWT_REFRESH_SECRET!,
    expiresIn: '7d', // 7 days
    algorithm: 'HS256' as const,
  },
};

export interface JWTPayload {
  userId: string;
  email: string;
  type: 'access' | 'refresh';
  iat: number;
  exp: number;
}
```

**Token Generation Service:**

```typescript
// packages/auth/src/services/token.service.ts
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { redisClient } from '@dating-app/shared/redis';
import { JWT_CONFIG, JWTPayload } from '../config/jwt.config';

export class TokenService {
  /**
   * Generate access and refresh token pair
   */
  static async generateTokenPair(userId: string, email: string) {
    const tokenId = uuidv4(); // Unique token ID for rotation tracking

    // Generate access token
    const accessToken = jwt.sign(
      { userId, email, type: 'access' } as Omit<JWTPayload, 'iat' | 'exp'>,
      JWT_CONFIG.access.secret,
      {
        expiresIn: JWT_CONFIG.access.expiresIn,
        algorithm: JWT_CONFIG.access.algorithm,
      }
    );

    // Generate refresh token
    const refreshToken = jwt.sign(
      { userId, email, type: 'refresh', tokenId } as Omit<JWTPayload, 'iat' | 'exp'> & { tokenId: string },
      JWT_CONFIG.refresh.secret,
      {
        expiresIn: JWT_CONFIG.refresh.expiresIn,
        algorithm: JWT_CONFIG.refresh.algorithm,
      }
    );

    // Store refresh token in Redis with expiry (for revocation capability)
    const refreshTokenKey = `refresh_token:${userId}:${tokenId}`;
    await redisClient.setex(
      refreshTokenKey,
      7 * 24 * 60 * 60, // 7 days in seconds
      JSON.stringify({ tokenId, createdAt: new Date().toISOString() })
    );

    return { accessToken, refreshToken, tokenId };
  }

  /**
   * Verify access token
   */
  static async verifyAccessToken(token: string): Promise<JWTPayload> {
    try {
      const payload = jwt.verify(token, JWT_CONFIG.access.secret) as JWTPayload;

      if (payload.type !== 'access') {
        throw new Error('Invalid token type');
      }

      return payload;
    } catch (error) {
      if (error instanceof jwt.TokenExpiredError) {
        throw new Error('Access token expired');
      }
      throw new Error('Invalid access token');
    }
  }

  /**
   * Verify refresh token and check if revoked
   */
  static async verifyRefreshToken(token: string): Promise<JWTPayload & { tokenId: string }> {
    try {
      const payload = jwt.verify(token, JWT_CONFIG.refresh.secret) as JWTPayload & { tokenId: string };

      if (payload.type !== 'refresh') {
        throw new Error('Invalid token type');
      }

      // Check if token has been revoked
      const refreshTokenKey = `refresh_token:${payload.userId}:${payload.tokenId}`;
      const tokenData = await redisClient.get(refreshTokenKey);

      if (!tokenData) {
        throw new Error('Refresh token has been revoked');
      }

      return payload;
    } catch (error) {
      if (error instanceof jwt.TokenExpiredError) {
        throw new Error('Refresh token expired');
      }
      throw new Error('Invalid refresh token');
    }
  }

  /**
   * Revoke refresh token (logout)
   */
  static async revokeRefreshToken(userId: string, tokenId: string): Promise<void> {
    const refreshTokenKey = `refresh_token:${userId}:${tokenId}`;
    await redisClient.del(refreshTokenKey);
  }

  /**
   * Revoke all user refresh tokens (logout from all devices)
   */
  static async revokeAllUserTokens(userId: string): Promise<void> {
    const pattern = `refresh_token:${userId}:*`;
    const keys = await redisClient.keys(pattern);

    if (keys.length > 0) {
      await redisClient.del(...keys);
    }
  }
}
```

**Authentication Controller:**

```typescript
// packages/auth/src/controllers/auth.controller.ts
import { Request, Response } from 'express';
import { TokenService } from '../services/token.service';
import { UserRepository } from '../repositories/user.repository';
import { PasswordService } from '../services/password.service';

export class AuthController {
  /**
   * Register new user
   */
  static async register(req: Request, res: Response) {
    try {
      const { email, password, firstName, dateOfBirth, gender } = req.body;

      // Check if user exists
      const existingUser = await UserRepository.findByEmail(email);
      if (existingUser) {
        return res.status(409).json({ error: 'User already exists' });
      }

      // Hash password
      const passwordHash = await PasswordService.hash(password);

      // Create user
      const user = await UserRepository.create({
        email,
        passwordHash,
        firstName,
        dateOfBirth: new Date(dateOfBirth),
        gender,
      });

      // Generate tokens
      const { accessToken, refreshToken, tokenId } = await TokenService.generateTokenPair(
        user.id,
        user.email
      );

      // Set refresh token in httpOnly cookie
      res.cookie('refreshToken', refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production', // HTTPS only in production
        sameSite: 'strict',
        maxAge: 7 * 24 * 60 * 60 * 1000, // 7 days
      });

      return res.status(201).json({
        user: {
          id: user.id,
          email: user.email,
          firstName: user.firstName,
        },
        accessToken,
      });
    } catch (error) {
      console.error('Register error:', error);
      return res.status(500).json({ error: 'Internal server error' });
    }
  }

  /**
   * Login
   */
  static async login(req: Request, res: Response) {
    try {
      const { email, password } = req.body;

      // Find user
      const user = await UserRepository.findByEmail(email);
      if (!user) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      // Verify password
      const isValidPassword = await PasswordService.verify(password, user.passwordHash);
      if (!isValidPassword) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      // Generate tokens
      const { accessToken, refreshToken } = await TokenService.generateTokenPair(
        user.id,
        user.email
      );

      // Set refresh token in httpOnly cookie
      res.cookie('refreshToken', refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 7 * 24 * 60 * 60 * 1000,
      });

      return res.status(200).json({
        user: {
          id: user.id,
          email: user.email,
          firstName: user.firstName,
        },
        accessToken,
      });
    } catch (error) {
      console.error('Login error:', error);
      return res.status(500).json({ error: 'Internal server error' });
    }
  }

  /**
   * Refresh access token
   */
  static async refresh(req: Request, res: Response) {
    try {
      const refreshToken = req.cookies.refreshToken;

      if (!refreshToken) {
        return res.status(401).json({ error: 'Refresh token not provided' });
      }

      // Verify refresh token
      const payload = await TokenService.verifyRefreshToken(refreshToken);

      // Revoke old refresh token (token rotation)
      await TokenService.revokeRefreshToken(payload.userId, payload.tokenId);

      // Generate new token pair
      const { accessToken, refreshToken: newRefreshToken } = await TokenService.generateTokenPair(
        payload.userId,
        payload.email
      );

      // Set new refresh token in cookie
      res.cookie('refreshToken', newRefreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 7 * 24 * 60 * 60 * 1000,
      });

      return res.status(200).json({ accessToken });
    } catch (error) {
      console.error('Refresh error:', error);
      return res.status(401).json({ error: 'Invalid refresh token' });
    }
  }

  /**
   * Logout (revoke refresh token)
   */
  static async logout(req: Request, res: Response) {
    try {
      const refreshToken = req.cookies.refreshToken;

      if (refreshToken) {
        const payload = await TokenService.verifyRefreshToken(refreshToken);
        await TokenService.revokeRefreshToken(payload.userId, payload.tokenId);
      }

      // Clear cookie
      res.clearCookie('refreshToken');

      return res.status(200).json({ message: 'Logged out successfully' });
    } catch (error) {
      console.error('Logout error:', error);
      return res.status(500).json({ error: 'Internal server error' });
    }
  }

  /**
   * Logout from all devices
   */
  static async logoutAll(req: Request, res: Response) {
    try {
      const userId = req.user!.userId; // From auth middleware

      await TokenService.revokeAllUserTokens(userId);

      // Clear cookie
      res.clearCookie('refreshToken');

      return res.status(200).json({ message: 'Logged out from all devices' });
    } catch (error) {
      console.error('Logout all error:', error);
      return res.status(500).json({ error: 'Internal server error' });
    }
  }
}
```

**Authentication Middleware:**

```typescript
// packages/auth/src/middleware/auth.middleware.ts
import { Request, Response, NextFunction } from 'express';
import { TokenService } from '../services/token.service';

// Extend Express Request type
declare global {
  namespace Express {
    interface Request {
      user?: {
        userId: string;
        email: string;
      };
    }
  }
}

export async function authenticateToken(req: Request, res: Response, next: NextFunction) {
  try {
    // Get token from Authorization header
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(' ')[1]; // Bearer <token>

    if (!token) {
      return res.status(401).json({ error: 'Access token required' });
    }

    // Verify token
    const payload = await TokenService.verifyAccessToken(token);

    // Attach user to request
    req.user = {
      userId: payload.userId,
      email: payload.email,
    };

    next();
  } catch (error) {
    if (error instanceof Error && error.message === 'Access token expired') {
      return res.status(401).json({ error: 'Access token expired', code: 'TOKEN_EXPIRED' });
    }
    return res.status(403).json({ error: 'Invalid access token' });
  }
}

/**
 * Optional authentication - doesn't fail if no token provided
 */
export async function optionalAuthentication(req: Request, res: Response, next: NextFunction) {
  try {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(' ')[1];

    if (token) {
      const payload = await TokenService.verifyAccessToken(token);
      req.user = {
        userId: payload.userId,
        email: payload.email,
      };
    }

    next();
  } catch (error) {
    // Continue without authentication
    next();
  }
}
```

---

## Authorization

### Role-Based Access Control (RBAC)

```typescript
// packages/auth/src/types/roles.ts
export enum Role {
  USER = 'user',
  MODERATOR = 'moderator',
  ADMIN = 'admin',
}

export const ROLE_HIERARCHY = {
  [Role.USER]: 0,
  [Role.MODERATOR]: 1,
  [Role.ADMIN]: 2,
};

// packages/auth/src/middleware/authorization.middleware.ts
import { Request, Response, NextFunction } from 'express';
import { Role, ROLE_HIERARCHY } from '../types/roles';
import { UserRepository } from '../repositories/user.repository';

/**
 * Require specific role or higher
 */
export function requireRole(minRole: Role) {
  return async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.user?.userId;

      if (!userId) {
        return res.status(401).json({ error: 'Authentication required' });
      }

      // Get user's role from database
      const user = await UserRepository.findById(userId);
      if (!user) {
        return res.status(403).json({ error: 'User not found' });
      }

      const userRole = user.role as Role;
      const userRoleLevel = ROLE_HIERARCHY[userRole];
      const minRoleLevel = ROLE_HIERARCHY[minRole];

      if (userRoleLevel < minRoleLevel) {
        return res.status(403).json({
          error: 'Insufficient permissions',
          required: minRole,
          current: userRole,
        });
      }

      next();
    } catch (error) {
      console.error('Authorization error:', error);
      return res.status(500).json({ error: 'Internal server error' });
    }
  };
}

/**
 * Check if user owns resource
 */
export function requireOwnership(resourceUserIdGetter: (req: Request) => Promise<string>) {
  return async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.user?.userId;

      if (!userId) {
        return res.status(401).json({ error: 'Authentication required' });
      }

      const resourceUserId = await resourceUserIdGetter(req);

      if (resourceUserId !== userId) {
        return res.status(403).json({ error: 'You do not own this resource' });
      }

      next();
    } catch (error) {
      console.error('Ownership check error:', error);
      return res.status(500).json({ error: 'Internal server error' });
    }
  };
}

// Usage example
// router.delete('/profiles/:id',
//   authenticateToken,
//   requireOwnership(async (req) => {
//     const profile = await ProfileRepository.findById(req.params.id);
//     return profile.userId;
//   }),
//   ProfileController.delete
// );
```

---

## Rate Limiting

### Sliding Window Rate Limiter with Redis

**Why Sliding Window:**
- More accurate than fixed window (prevents burst at window boundaries)
- Fair rate limiting across time
- Prevents coordinated attacks

**Implementation:**

```typescript
// packages/shared/src/middleware/rate-limiter.ts
import { Request, Response, NextFunction } from 'express';
import { redisClient } from '../config/redis';

export interface RateLimitConfig {
  windowMs: number;      // Time window in milliseconds
  maxRequests: number;   // Max requests per window
  keyGenerator?: (req: Request) => string;  // Custom key generator
  skipSuccessfulRequests?: boolean;  // Only count failed requests
  skipFailedRequests?: boolean;      // Only count successful requests
}

export class SlidingWindowRateLimiter {
  private config: Required<RateLimitConfig>;

  constructor(config: RateLimitConfig) {
    this.config = {
      ...config,
      keyGenerator: config.keyGenerator || this.defaultKeyGenerator,
      skipSuccessfulRequests: config.skipSuccessfulRequests || false,
      skipFailedRequests: config.skipFailedRequests || false,
    };
  }

  private defaultKeyGenerator(req: Request): string {
    // Use IP address as default key
    return req.ip || req.socket.remoteAddress || 'unknown';
  }

  middleware() {
    return async (req: Request, res: Response, next: NextFunction) => {
      try {
        const key = `rate_limit:${this.config.keyGenerator(req)}`;
        const now = Date.now();
        const windowStart = now - this.config.windowMs;

        // Use Redis sorted set to store requests with timestamp as score
        const multi = redisClient.multi();

        // Remove old requests outside the window
        multi.zremrangebyscore(key, 0, windowStart);

        // Count requests in current window
        multi.zcard(key);

        // Add current request
        multi.zadd(key, now, `${now}-${Math.random()}`);

        // Set expiry on key
        multi.expire(key, Math.ceil(this.config.windowMs / 1000));

        const results = await multi.exec();
        const requestCount = results?.[1]?.[1] as number || 0;

        // Set rate limit headers
        res.setHeader('X-RateLimit-Limit', this.config.maxRequests);
        res.setHeader('X-RateLimit-Remaining', Math.max(0, this.config.maxRequests - requestCount - 1));
        res.setHeader('X-RateLimit-Reset', new Date(now + this.config.windowMs).toISOString());

        if (requestCount >= this.config.maxRequests) {
          const retryAfter = Math.ceil(this.config.windowMs / 1000);
          res.setHeader('Retry-After', retryAfter);

          return res.status(429).json({
            error: 'Too many requests',
            retryAfter,
            limit: this.config.maxRequests,
            window: `${this.config.windowMs / 1000}s`,
          });
        }

        // Handle response to conditionally count request
        const originalSend = res.send;
        res.send = function(data) {
          const shouldCount =
            (!this.config.skipSuccessfulRequests || res.statusCode >= 400) &&
            (!this.config.skipFailedRequests || res.statusCode < 400);

          if (!shouldCount) {
            // Remove the request we added if we shouldn't count it
            redisClient.zrem(key, `${now}-${Math.random()}`);
          }

          return originalSend.call(this, data);
        }.bind(this);

        next();
      } catch (error) {
        console.error('Rate limiter error:', error);
        // Fail open - allow request if rate limiter fails
        next();
      }
    };
  }
}

// Pre-configured rate limiters for different endpoints
export const rateLimiters = {
  // Strict for auth endpoints (prevent brute force)
  auth: new SlidingWindowRateLimiter({
    windowMs: 15 * 60 * 1000, // 15 minutes
    maxRequests: 5,
    skipSuccessfulRequests: true, // Only count failed login attempts
  }),

  // Moderate for API endpoints
  api: new SlidingWindowRateLimiter({
    windowMs: 60 * 1000, // 1 minute
    maxRequests: 60,
  }),

  // Lenient for swipe actions
  swipe: new SlidingWindowRateLimiter({
    windowMs: 60 * 1000, // 1 minute
    maxRequests: 100,
  }),

  // Very strict for resource-intensive operations
  imageUpload: new SlidingWindowRateLimiter({
    windowMs: 60 * 60 * 1000, // 1 hour
    maxRequests: 10,
  }),

  // Per-user rate limiting for messages
  messaging: new SlidingWindowRateLimiter({
    windowMs: 60 * 1000, // 1 minute
    maxRequests: 30,
    keyGenerator: (req) => `user:${req.user?.userId || req.ip}`,
  }),
};

// Usage in routes:
// router.post('/auth/login', rateLimiters.auth.middleware(), AuthController.login);
// router.post('/swipe', authenticateToken, rateLimiters.swipe.middleware(), SwipeController.swipe);
```

---

## Password Hashing

### Argon2 Implementation (Recommended)

**Why Argon2 over bcrypt:**
- Memory-hard algorithm (resistant to GPU/ASIC attacks)
- Won Password Hashing Competition (2015)
- Configurable memory, time, and parallelism
- OWASP and NIST recommended

```typescript
// packages/auth/src/services/password.service.ts
import argon2 from 'argon2';

export class PasswordService {
  private static readonly ARGON2_OPTIONS = {
    type: argon2.argon2id, // Hybrid of argon2i and argon2d
    memoryCost: 65536,     // 64 MB
    timeCost: 3,           // 3 iterations
    parallelism: 4,        // 4 threads
  };

  /**
   * Hash password using Argon2id
   */
  static async hash(password: string): Promise<string> {
    return argon2.hash(password, this.ARGON2_OPTIONS);
  }

  /**
   * Verify password against hash
   */
  static async verify(password: string, hash: string): Promise<boolean> {
    try {
      return await argon2.verify(hash, password);
    } catch (error) {
      console.error('Password verification error:', error);
      return false;
    }
  }

  /**
   * Check if hash needs rehashing (algorithm params changed)
   */
  static needsRehash(hash: string): boolean {
    return argon2.needsRehash(hash, this.ARGON2_OPTIONS);
  }

  /**
   * Validate password strength
   */
  static validatePasswordStrength(password: string): {
    valid: boolean;
    errors: string[];
  } {
    const errors: string[] = [];

    if (password.length < 8) {
      errors.push('Password must be at least 8 characters');
    }

    if (password.length > 128) {
      errors.push('Password must be less than 128 characters');
    }

    if (!/[a-z]/.test(password)) {
      errors.push('Password must contain at least one lowercase letter');
    }

    if (!/[A-Z]/.test(password)) {
      errors.push('Password must contain at least one uppercase letter');
    }

    if (!/\d/.test(password)) {
      errors.push('Password must contain at least one number');
    }

    if (!/[^a-zA-Z0-9]/.test(password)) {
      errors.push('Password must contain at least one special character');
    }

    // Check against common passwords (simplified - use a proper library in production)
    const commonPasswords = ['password', '12345678', 'qwerty', 'abc123'];
    if (commonPasswords.includes(password.toLowerCase())) {
      errors.push('Password is too common');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }
}
```

**Bcrypt Alternative (for legacy compatibility):**

```typescript
// Alternative implementation with bcrypt
import bcrypt from 'bcrypt';

export class BcryptPasswordService {
  private static readonly SALT_ROUNDS = 12; // Higher = more secure but slower

  static async hash(password: string): Promise<string> {
    return bcrypt.hash(password, this.SALT_ROUNDS);
  }

  static async verify(password: string, hash: string): Promise<boolean> {
    try {
      return await bcrypt.compare(password, hash);
    } catch (error) {
      console.error('Password verification error:', error);
      return false;
    }
  }
}
```

---

## OAuth 2.0 Social Login

### Google, Facebook, and Apple Sign-In

```typescript
// packages/auth/src/config/oauth.config.ts
export const OAUTH_CONFIG = {
  google: {
    clientID: process.env.GOOGLE_CLIENT_ID!,
    clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
    callbackURL: `${process.env.API_URL}/auth/google/callback`,
  },
  facebook: {
    clientID: process.env.FACEBOOK_APP_ID!,
    clientSecret: process.env.FACEBOOK_APP_SECRET!,
    callbackURL: `${process.env.API_URL}/auth/facebook/callback`,
    profileFields: ['id', 'emails', 'name', 'picture.type(large)'],
  },
  apple: {
    clientID: process.env.APPLE_CLIENT_ID!,
    teamID: process.env.APPLE_TEAM_ID!,
    keyID: process.env.APPLE_KEY_ID!,
    privateKey: process.env.APPLE_PRIVATE_KEY!,
    callbackURL: `${process.env.API_URL}/auth/apple/callback`,
  },
};

// packages/auth/src/strategies/passport.config.ts
import passport from 'passport';
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import { Strategy as FacebookStrategy } from 'passport-facebook';
import { Strategy as AppleStrategy } from 'passport-apple';
import { OAUTH_CONFIG } from '../config/oauth.config';
import { UserRepository } from '../repositories/user.repository';

/**
 * Google OAuth Strategy
 */
passport.use(
  new GoogleStrategy(
    {
      clientID: OAUTH_CONFIG.google.clientID,
      clientSecret: OAUTH_CONFIG.google.clientSecret,
      callbackURL: OAUTH_CONFIG.google.callbackURL,
      scope: ['profile', 'email'],
    },
    async (accessToken, refreshToken, profile, done) => {
      try {
        // Check if user exists with this Google ID
        let user = await UserRepository.findByOAuthProvider('google', profile.id);

        if (!user) {
          // Check if user exists with this email
          const email = profile.emails?.[0]?.value;
          if (email) {
            user = await UserRepository.findByEmail(email);

            if (user) {
              // Link Google account to existing user
              await UserRepository.linkOAuthProvider(user.id, 'google', profile.id);
            }
          }
        }

        if (!user) {
          // Create new user
          user = await UserRepository.create({
            email: profile.emails?.[0]?.value!,
            firstName: profile.name?.givenName || profile.displayName,
            lastName: profile.name?.familyName,
            oauthProvider: 'google',
            oauthProviderId: profile.id,
            profilePicture: profile.photos?.[0]?.value,
            emailVerified: true, // Google emails are pre-verified
          });
        }

        return done(null, user);
      } catch (error) {
        return done(error as Error, undefined);
      }
    }
  )
);

/**
 * Facebook OAuth Strategy
 */
passport.use(
  new FacebookStrategy(
    {
      clientID: OAUTH_CONFIG.facebook.clientID,
      clientSecret: OAUTH_CONFIG.facebook.clientSecret,
      callbackURL: OAUTH_CONFIG.facebook.callbackURL,
      profileFields: OAUTH_CONFIG.facebook.profileFields,
    },
    async (accessToken, refreshToken, profile, done) => {
      try {
        let user = await UserRepository.findByOAuthProvider('facebook', profile.id);

        if (!user) {
          const email = profile.emails?.[0]?.value;
          if (email) {
            user = await UserRepository.findByEmail(email);
            if (user) {
              await UserRepository.linkOAuthProvider(user.id, 'facebook', profile.id);
            }
          }
        }

        if (!user) {
          user = await UserRepository.create({
            email: profile.emails?.[0]?.value!,
            firstName: profile.name?.givenName || profile.displayName,
            lastName: profile.name?.familyName,
            oauthProvider: 'facebook',
            oauthProviderId: profile.id,
            profilePicture: profile.photos?.[0]?.value,
            emailVerified: true,
          });
        }

        return done(null, user);
      } catch (error) {
        return done(error as Error, undefined);
      }
    }
  )
);

/**
 * Apple OAuth Strategy
 */
passport.use(
  new AppleStrategy(
    {
      clientID: OAUTH_CONFIG.apple.clientID,
      teamID: OAUTH_CONFIG.apple.teamID,
      keyID: OAUTH_CONFIG.apple.keyID,
      privateKeyString: OAUTH_CONFIG.apple.privateKey,
      callbackURL: OAUTH_CONFIG.apple.callbackURL,
    },
    async (accessToken, refreshToken, idToken, profile, done) => {
      try {
        let user = await UserRepository.findByOAuthProvider('apple', profile.id);

        if (!user) {
          const email = profile.email;
          if (email) {
            user = await UserRepository.findByEmail(email);
            if (user) {
              await UserRepository.linkOAuthProvider(user.id, 'apple', profile.id);
            }
          }
        }

        if (!user) {
          // Apple provides name only on first sign-in
          user = await UserRepository.create({
            email: profile.email!,
            firstName: profile.name?.firstName || 'User',
            lastName: profile.name?.lastName,
            oauthProvider: 'apple',
            oauthProviderId: profile.id,
            emailVerified: true,
          });
        }

        return done(null, user);
      } catch (error) {
        return done(error as Error, undefined);
      }
    }
  )
);

export default passport;
```

**OAuth Routes:**

```typescript
// packages/auth/src/routes/oauth.routes.ts
import { Router } from 'express';
import passport from '../strategies/passport.config';
import { TokenService } from '../services/token.service';

const router = Router();

// Google OAuth
router.get('/google', passport.authenticate('google'));

router.get(
  '/google/callback',
  passport.authenticate('google', { session: false, failureRedirect: '/login' }),
  async (req, res) => {
    const user = req.user as any;
    const { accessToken, refreshToken } = await TokenService.generateTokenPair(user.id, user.email);

    res.cookie('refreshToken', refreshToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 7 * 24 * 60 * 60 * 1000,
    });

    // Redirect to frontend with access token
    res.redirect(`${process.env.FRONTEND_URL}/auth/callback?token=${accessToken}`);
  }
);

// Facebook OAuth
router.get('/facebook', passport.authenticate('facebook', { scope: ['email'] }));

router.get(
  '/facebook/callback',
  passport.authenticate('facebook', { session: false, failureRedirect: '/login' }),
  async (req, res) => {
    const user = req.user as any;
    const { accessToken, refreshToken } = await TokenService.generateTokenPair(user.id, user.email);

    res.cookie('refreshToken', refreshToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 7 * 24 * 60 * 60 * 1000,
    });

    res.redirect(`${process.env.FRONTEND_URL}/auth/callback?token=${accessToken}`);
  }
);

// Apple OAuth
router.post('/apple', passport.authenticate('apple'));

router.post(
  '/apple/callback',
  passport.authenticate('apple', { session: false, failureRedirect: '/login' }),
  async (req, res) => {
    const user = req.user as any;
    const { accessToken, refreshToken } = await TokenService.generateTokenPair(user.id, user.email);

    res.cookie('refreshToken', refreshToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 7 * 24 * 60 * 60 * 1000,
    });

    res.json({ accessToken, user: { id: user.id, email: user.email } });
  }
);

export default router;
```

---

## Input Validation & Sanitization

### express-validator Implementation

```typescript
// packages/shared/src/middleware/validation.middleware.ts
import { body, param, query, validationResult } from 'express-validator';
import { Request, Response, NextFunction } from 'express';

/**
 * Validation error handler middleware
 */
export function handleValidationErrors(req: Request, res: Response, next: NextFunction) {
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    return res.status(400).json({
      error: 'Validation failed',
      details: errors.array().map(err => ({
        field: err.type === 'field' ? err.path : undefined,
        message: err.msg,
      })),
    });
  }

  next();
}

/**
 * Common validation rules
 */
export const validationRules = {
  // User registration
  register: [
    body('email')
      .isEmail()
      .normalizeEmail()
      .withMessage('Invalid email address'),
    body('password')
      .isLength({ min: 8, max: 128 })
      .withMessage('Password must be 8-128 characters')
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)
      .withMessage('Password must contain uppercase, lowercase, number, and special character'),
    body('firstName')
      .trim()
      .isLength({ min: 1, max: 50 })
      .matches(/^[a-zA-Z\s'-]+$/)
      .withMessage('Invalid first name'),
    body('dateOfBirth')
      .isISO8601()
      .custom((value) => {
        const age = new Date().getFullYear() - new Date(value).getFullYear();
        if (age < 18) throw new Error('Must be 18 or older');
        if (age > 100) throw new Error('Invalid date of birth');
        return true;
      }),
    body('gender')
      .isIn(['male', 'female', 'non-binary', 'other'])
      .withMessage('Invalid gender'),
  ],

  // Login
  login: [
    body('email').isEmail().normalizeEmail(),
    body('password').notEmpty().withMessage('Password is required'),
  ],

  // Profile update
  updateProfile: [
    body('bio')
      .optional()
      .trim()
      .isLength({ max: 500 })
      .withMessage('Bio must be less than 500 characters')
      .escape(), // Escape HTML entities
    body('interests')
      .optional()
      .isArray({ max: 10 })
      .withMessage('Maximum 10 interests allowed'),
    body('interests.*')
      .trim()
      .isLength({ min: 1, max: 30 })
      .matches(/^[a-zA-Z0-9\s-]+$/)
      .withMessage('Invalid interest format'),
    body('height')
      .optional()
      .isInt({ min: 100, max: 250 })
      .withMessage('Height must be between 100-250 cm'),
    body('location.latitude')
      .optional()
      .isFloat({ min: -90, max: 90 })
      .withMessage('Invalid latitude'),
    body('location.longitude')
      .optional()
      .isFloat({ min: -180, max: 180 })
      .withMessage('Invalid longitude'),
  ],

  // Message sending
  sendMessage: [
    body('matchId')
      .isUUID()
      .withMessage('Invalid match ID'),
    body('content')
      .trim()
      .notEmpty()
      .withMessage('Message cannot be empty')
      .isLength({ max: 1000 })
      .withMessage('Message too long (max 1000 characters)')
      .escape(),
  ],

  // Swipe action
  swipe: [
    body('targetUserId')
      .isUUID()
      .withMessage('Invalid user ID'),
    body('action')
      .isIn(['like', 'dislike', 'superlike'])
      .withMessage('Invalid swipe action'),
  ],

  // Report user
  reportUser: [
    body('reportedUserId')
      .isUUID()
      .withMessage('Invalid user ID'),
    body('reason')
      .isIn(['inappropriate_content', 'fake_profile', 'harassment', 'spam', 'other'])
      .withMessage('Invalid report reason'),
    body('description')
      .optional()
      .trim()
      .isLength({ max: 500 })
      .withMessage('Description too long')
      .escape(),
  ],

  // Pagination
  pagination: [
    query('page')
      .optional()
      .isInt({ min: 1 })
      .withMessage('Page must be positive integer')
      .toInt(),
    query('limit')
      .optional()
      .isInt({ min: 1, max: 100 })
      .withMessage('Limit must be between 1-100')
      .toInt(),
  ],

  // UUID parameter
  uuidParam: (paramName: string) => [
    param(paramName)
      .isUUID()
      .withMessage(`Invalid ${paramName}`),
  ],
};

// Usage in routes:
// router.post('/register', validationRules.register, handleValidationErrors, AuthController.register);
// router.patch('/profile', authenticateToken, validationRules.updateProfile, handleValidationErrors, ProfileController.update);
```

### Custom Sanitization Functions

```typescript
// packages/shared/src/utils/sanitization.ts
import xss from 'xss';
import validator from 'validator';

export class Sanitizer {
  /**
   * Sanitize HTML content (allow safe tags only)
   */
  static sanitizeHtml(content: string, allowedTags?: string[]): string {
    return xss(content, {
      whiteList: allowedTags ?
        Object.fromEntries(allowedTags.map(tag => [tag, []])) :
        {}, // No tags allowed by default
    });
  }

  /**
   * Strip all HTML tags
   */
  static stripHtml(content: string): string {
    return validator.stripLow(content.replace(/<[^>]*>/g, ''));
  }

  /**
   * Sanitize user bio
   */
  static sanitizeBio(bio: string): string {
    return this.stripHtml(bio)
      .trim()
      .substring(0, 500); // Max 500 characters
  }

  /**
   * Sanitize and normalize phone number
   */
  static sanitizePhone(phone: string): string | null {
    // Remove all non-numeric characters
    const cleaned = phone.replace(/\D/g, '');

    // Validate length (assuming international format)
    if (cleaned.length < 10 || cleaned.length > 15) {
      return null;
    }

    return cleaned;
  }

  /**
   * Sanitize URL
   */
  static sanitizeUrl(url: string): string | null {
    if (!validator.isURL(url, { protocols: ['http', 'https'] })) {
      return null;
    }
    return validator.trim(url);
  }

  /**
   * Sanitize search query
   */
  static sanitizeSearchQuery(query: string): string {
    return this.stripHtml(query)
      .trim()
      .replace(/[^\w\s-]/g, '') // Keep only alphanumeric, spaces, hyphens
      .substring(0, 100);
  }
}
```

---

## Security Headers

### Helmet.js Configuration

```typescript
// packages/api-gateway/src/middleware/security-headers.ts
import helmet from 'helmet';
import { Express } from 'express';

export function configureSecurityHeaders(app: Express) {
  // Use Helmet with custom configuration
  app.use(
    helmet({
      // Content Security Policy
      contentSecurityPolicy: {
        directives: {
          defaultSrc: ["'self'"],
          scriptSrc: ["'self'", "'unsafe-inline'"], // Avoid unsafe-inline in production
          styleSrc: ["'self'", "'unsafe-inline'", 'https://fonts.googleapis.com'],
          fontSrc: ["'self'", 'https://fonts.gstatic.com'],
          imgSrc: ["'self'", 'data:', 'https:', 'blob:'],
          connectSrc: ["'self'", process.env.API_URL!],
          frameSrc: ["'none'"],
          objectSrc: ["'none'"],
          upgradeInsecureRequests: process.env.NODE_ENV === 'production' ? [] : null,
        },
      },

      // Strict Transport Security (HSTS)
      hsts: {
        maxAge: 31536000, // 1 year in seconds
        includeSubDomains: true,
        preload: true,
      },

      // Referrer Policy
      referrerPolicy: {
        policy: 'strict-origin-when-cross-origin',
      },

      // X-Frame-Options (prevent clickjacking)
      frameguard: {
        action: 'deny',
      },

      // X-Content-Type-Options (prevent MIME sniffing)
      noSniff: true,

      // X-XSS-Protection (legacy, modern browsers use CSP)
      xssFilter: true,

      // Remove X-Powered-By header
      hidePoweredBy: true,

      // Permissions Policy (formerly Feature Policy)
      permittedCrossDomainPolicies: {
        permittedPolicies: 'none',
      },
    })
  );

  // Additional security headers
  app.use((req, res, next) => {
    // Permissions Policy
    res.setHeader(
      'Permissions-Policy',
      'geolocation=(self), microphone=(), camera=()'
    );

    // X-Download-Options (IE8+)
    res.setHeader('X-Download-Options', 'noopen');

    // X-Permitted-Cross-Domain-Policies
    res.setHeader('X-Permitted-Cross-Domain-Policies', 'none');

    next();
  });
}
```

---

## Encryption

### Data Encryption at Rest

```typescript
// packages/shared/src/services/encryption.service.ts
import crypto from 'crypto';

export class EncryptionService {
  private static readonly ALGORITHM = 'aes-256-gcm';
  private static readonly KEY = Buffer.from(process.env.ENCRYPTION_KEY!, 'hex'); // 32 bytes
  private static readonly IV_LENGTH = 16; // AES block size
  private static readonly AUTH_TAG_LENGTH = 16;

  /**
   * Encrypt sensitive data
   */
  static encrypt(plaintext: string): string {
    const iv = crypto.randomBytes(this.IV_LENGTH);
    const cipher = crypto.createCipheriv(this.ALGORITHM, this.KEY, iv);

    let encrypted = cipher.update(plaintext, 'utf8', 'hex');
    encrypted += cipher.final('hex');

    const authTag = cipher.getAuthTag();

    // Format: iv:authTag:ciphertext
    return `${iv.toString('hex')}:${authTag.toString('hex')}:${encrypted}`;
  }

  /**
   * Decrypt sensitive data
   */
  static decrypt(ciphertext: string): string {
    const [ivHex, authTagHex, encrypted] = ciphertext.split(':');

    const iv = Buffer.from(ivHex, 'hex');
    const authTag = Buffer.from(authTagHex, 'hex');

    const decipher = crypto.createDecipheriv(this.ALGORITHM, this.KEY, iv);
    decipher.setAuthTag(authTag);

    let decrypted = decipher.update(encrypted, 'hex', 'utf8');
    decrypted += decipher.final('utf8');

    return decrypted;
  }

  /**
   * Hash data one-way (for verification, not decryption)
   */
  static hash(data: string): string {
    return crypto.createHash('sha256').update(data).digest('hex');
  }

  /**
   * Generate secure random token
   */
  static generateToken(length: number = 32): string {
    return crypto.randomBytes(length).toString('hex');
  }

  /**
   * Encrypt email for privacy (used in analytics/logs)
   */
  static encryptEmail(email: string): string {
    return this.encrypt(email);
  }

  /**
   * Encrypt phone number
   */
  static encryptPhone(phone: string): string {
    return this.encrypt(phone);
  }
}

// Usage example:
// const encryptedEmail = EncryptionService.encrypt(user.email);
// await db.user.update({ where: { id }, data: { emailEncrypted: encryptedEmail } });
```

### TLS/SSL Configuration

```typescript
// packages/api-gateway/src/server.ts
import https from 'https';
import fs from 'fs';
import express from 'express';

const app = express();

if (process.env.NODE_ENV === 'production') {
  // HTTPS server configuration
  const httpsOptions = {
    key: fs.readFileSync(process.env.SSL_KEY_PATH!),
    cert: fs.readFileSync(process.env.SSL_CERT_PATH!),
    ca: fs.readFileSync(process.env.SSL_CA_PATH!), // Certificate Authority

    // TLS 1.3 only (most secure)
    minVersion: 'TLSv1.3' as const,

    // Cipher suites (ordered by preference)
    ciphers: [
      'TLS_AES_256_GCM_SHA384',
      'TLS_CHACHA20_POLY1305_SHA256',
      'TLS_AES_128_GCM_SHA256',
    ].join(':'),

    // Prefer server cipher order
    honorCipherOrder: true,
  };

  const server = https.createServer(httpsOptions, app);
  server.listen(443, () => {
    console.log('HTTPS server listening on port 443');
  });
} else {
  // HTTP for development
  app.listen(3000, () => {
    console.log('HTTP server listening on port 3000');
  });
}
```

---

## Complete Security Checklist

### Environment Variables (.env.example)

```bash
# JWT Secrets (generate with: openssl rand -hex 32)
JWT_ACCESS_SECRET=your_access_token_secret_here
JWT_REFRESH_SECRET=your_refresh_token_secret_here

# Encryption Key (generate with: openssl rand -hex 32)
ENCRYPTION_KEY=your_encryption_key_here

# OAuth Credentials
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
FACEBOOK_APP_ID=your_facebook_app_id
FACEBOOK_APP_SECRET=your_facebook_app_secret
APPLE_CLIENT_ID=your_apple_client_id
APPLE_TEAM_ID=your_apple_team_id
APPLE_KEY_ID=your_apple_key_id
APPLE_PRIVATE_KEY=your_apple_private_key

# SSL/TLS Certificates (production)
SSL_KEY_PATH=/path/to/private.key
SSL_CERT_PATH=/path/to/certificate.crt
SSL_CA_PATH=/path/to/ca_bundle.crt

# Database URLs
DATABASE_URL=postgresql://user:password@localhost:5432/dating_app
REDIS_URL=redis://localhost:6379

# API Configuration
API_URL=https://api.yourdatingapp.com
FRONTEND_URL=https://yourdatingapp.com
NODE_ENV=production
```

### Security Best Practices Summary

1. **Authentication:**
   - ✅ Short-lived access tokens (15 minutes)
   - ✅ Long-lived refresh tokens (7 days) in httpOnly cookies
   - ✅ Token rotation on refresh
   - ✅ Refresh token revocation capability
   - ✅ Logout from all devices support

2. **Password Security:**
   - ✅ Argon2id hashing (memory-hard, GPU-resistant)
   - ✅ Password strength validation
   - ✅ No password length limits (within reason)
   - ✅ Hash rehashing when algorithm parameters change

3. **Rate Limiting:**
   - ✅ Sliding window algorithm (fair, accurate)
   - ✅ Per-endpoint configuration
   - ✅ Per-user and per-IP limiting
   - ✅ Conditional counting (e.g., only failed login attempts)

4. **OAuth:**
   - ✅ Google, Facebook, Apple integration
   - ✅ Account linking for existing users
   - ✅ Consistent token handling

5. **Input Validation:**
   - ✅ Server-side validation (never trust client)
   - ✅ Type checking and sanitization
   - ✅ HTML escaping
   - ✅ SQL injection prevention (ORM usage)

6. **Security Headers:**
   - ✅ CSP (Content Security Policy)
   - ✅ HSTS (Strict Transport Security)
   - ✅ X-Frame-Options (clickjacking protection)
   - ✅ X-Content-Type-Options (MIME sniffing protection)

7. **Encryption:**
   - ✅ AES-256-GCM for data at rest
   - ✅ TLS 1.3 for data in transit
   - ✅ Secure random token generation
   - ✅ Email/phone encryption in database

8. **General:**
   - ✅ Environment variables for secrets
   - ✅ No hardcoded credentials
   - ✅ Principle of least privilege
   - ✅ Regular security audits
