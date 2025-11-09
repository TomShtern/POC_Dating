# Backend Code Organization & Architecture Patterns

## Overview

Advanced backend architecture patterns for scalable, maintainable Node.js/TypeScript dating app services.

**Focus**: Clean Architecture, Domain-Driven Design, Code organization, Testing patterns

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Clean Architecture Pattern](#clean-architecture-pattern)
3. [Domain-Driven Design](#domain-driven-design)
4. [Dependency Injection](#dependency-injection)
5. [Error Handling](#error-handling)
6. [Logging Strategy](#logging-strategy)
7. [Testing Patterns](#testing-patterns)
8. [API Versioning](#api-versioning)

---

## Project Structure

### Monorepo Structure (Recommended for Multiple Services)

```
dating-app/
├── packages/
│   ├── shared/                    # Shared code across services
│   │   ├── src/
│   │   │   ├── types/             # TypeScript types/interfaces
│   │   │   ├── utils/             # Utility functions
│   │   │   ├── constants/         # Shared constants
│   │   │   └── config/            # Configuration
│   │   ├── package.json
│   │   └── tsconfig.json
│   │
│   ├── api-gateway/               # API Gateway service
│   │   ├── src/
│   │   │   ├── routes/
│   │   │   ├── middleware/
│   │   │   └── index.ts
│   │   ├── package.json
│   │   └── Dockerfile
│   │
│   ├── user-service/              # User management
│   │   ├── src/
│   │   │   ├── domain/            # Domain layer (entities, value objects)
│   │   │   ├── application/       # Application layer (use cases)
│   │   │   ├── infrastructure/    # Infrastructure layer (DB, external APIs)
│   │   │   ├── presentation/      # Presentation layer (controllers, routes)
│   │   │   └── index.ts
│   │   ├── tests/
│   │   ├── prisma/
│   │   ├── package.json
│   │   └── Dockerfile
│   │
│   ├── match-service/             # Matching algorithm
│   ├── messaging-service/         # Real-time messaging
│   └── notification-service/      # Push notifications
│
├── docker-compose.yml
├── package.json                   # Root package.json with workspaces
├── tsconfig.json                  # Base TypeScript config
├── turbo.json                     # Turborepo configuration (optional)
└── README.md
```

**package.json (root):**
```json
{
  "name": "dating-app",
  "private": true,
  "workspaces": [
    "packages/*"
  ],
  "scripts": {
    "dev": "turbo run dev",
    "build": "turbo run build",
    "test": "turbo run test",
    "lint": "turbo run lint"
  },
  "devDependencies": {
    "turbo": "^1.10.0",
    "typescript": "^5.3.3"
  }
}
```

---

### Single Service Structure (Clean Architecture)

```
user-service/
├── src/
│   ├── domain/                    # Enterprise Business Rules
│   │   ├── entities/
│   │   │   ├── User.ts
│   │   │   ├── Profile.ts
│   │   │   └── Photo.ts
│   │   ├── value-objects/
│   │   │   ├── Email.ts
│   │   │   ├── Password.ts
│   │   │   └── Location.ts
│   │   ├── repositories/          # Repository interfaces
│   │   │   ├── IUserRepository.ts
│   │   │   └── IPhotoRepository.ts
│   │   └── errors/
│   │       ├── DomainError.ts
│   │       └── ValidationError.ts
│   │
│   ├── application/               # Application Business Rules
│   │   ├── use-cases/
│   │   │   ├── CreateUser/
│   │   │   │   ├── CreateUserUseCase.ts
│   │   │   │   ├── CreateUserDTO.ts
│   │   │   │   └── CreateUserUseCase.test.ts
│   │   │   ├── UpdateProfile/
│   │   │   └── UploadPhoto/
│   │   ├── services/
│   │   │   ├── EmailService.ts
│   │   │   └── ImageProcessingService.ts
│   │   └── mappers/
│   │       └── UserMapper.ts
│   │
│   ├── infrastructure/            # Frameworks & Drivers
│   │   ├── database/
│   │   │   ├── prisma/
│   │   │   │   └── schema.prisma
│   │   │   ├── repositories/
│   │   │   │   ├── PrismaUserRepository.ts
│   │   │   │   └── PrismaPhotoRepository.ts
│   │   │   └── migrations/
│   │   ├── cache/
│   │   │   └── RedisClient.ts
│   │   ├── storage/
│   │   │   └── S3StorageService.ts
│   │   ├── messaging/
│   │   │   └── KafkaProducer.ts
│   │   └── external-apis/
│   │       └── GoogleOAuthService.ts
│   │
│   ├── presentation/              # Interface Adapters
│   │   ├── http/
│   │   │   ├── controllers/
│   │   │   │   ├── UserController.ts
│   │   │   │   └── ProfileController.ts
│   │   │   ├── routes/
│   │   │   │   ├── userRoutes.ts
│   │   │   │   └── profileRoutes.ts
│   │   │   ├── middleware/
│   │   │   │   ├── authMiddleware.ts
│   │   │   │   ├── errorMiddleware.ts
│   │   │   │   └── validationMiddleware.ts
│   │   │   └── validators/
│   │   │       └── UserValidator.ts
│   │   └── websocket/
│   │       └── PresenceHandler.ts
│   │
│   ├── shared/
│   │   ├── types/
│   │   ├── utils/
│   │   ├── constants/
│   │   └── config/
│   │       └── index.ts
│   │
│   ├── di/                        # Dependency Injection
│   │   └── container.ts
│   │
│   └── index.ts                   # Application entry point
│
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
│
├── prisma/
├── package.json
├── tsconfig.json
├── jest.config.js
├── Dockerfile
└── README.md
```

---

## Clean Architecture Pattern

### Layers Overview

```
┌──────────────────────────────────────────────┐
│         PRESENTATION LAYER                   │  ← HTTP Controllers, WebSocket handlers
│         (Routes, Controllers)                │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│         APPLICATION LAYER                    │  ← Use Cases, Services
│         (Business Logic)                     │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│         DOMAIN LAYER                         │  ← Entities, Value Objects
│         (Core Business Rules)                │
└──────────────────────────────────────────────┘
                    ↑
┌──────────────────────────────────────────────┐
│         INFRASTRUCTURE LAYER                 │  ← Database, Cache, External APIs
│         (Frameworks & Drivers)               │
└──────────────────────────────────────────────┘
```

**Dependency Rule**: Source code dependencies point inward. Inner layers know nothing about outer layers.

---

### Domain Layer

**Entity Example:**

```typescript
// src/domain/entities/User.ts
import { Email } from '../value-objects/Email';
import { Password } from '../value-objects/Password';
import { DomainError } from '../errors/DomainError';

export interface UserProps {
  id: string;
  email: Email;
  passwordHash: string;
  firstName: string;
  dateOfBirth: Date;
  gender: 'male' | 'female' | 'other';
  bio?: string;
  verified: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export class User {
  private props: UserProps;

  private constructor(props: UserProps) {
    this.props = props;
  }

  // Factory method
  public static create(props: Omit<UserProps, 'id' | 'createdAt' | 'updatedAt'>): User {
    const user = new User({
      ...props,
      id: this.generateId(),
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    user.validate();
    return user;
  }

  // Reconstitute from database
  public static fromPersistence(props: UserProps): User {
    return new User(props);
  }

  private validate(): void {
    if (!this.props.email.isValid()) {
      throw new DomainError('Invalid email');
    }

    if (this.getAge() < 18) {
      throw new DomainError('User must be at least 18 years old');
    }

    if (this.props.firstName.length < 2) {
      throw new DomainError('First name must be at least 2 characters');
    }
  }

  // Business logic methods
  public updateProfile(firstName: string, bio?: string): void {
    this.props.firstName = firstName;
    this.props.bio = bio;
    this.props.updatedAt = new Date();
  }

  public verify(): void {
    this.props.verified = true;
    this.props.updatedAt = new Date();
  }

  public changePassword(newPasswordHash: string): void {
    this.props.passwordHash = newPasswordHash;
    this.props.updatedAt = new Date();
  }

  // Getters
  public get id(): string {
    return this.props.id;
  }

  public get email(): Email {
    return this.props.email;
  }

  public get passwordHash(): string {
    return this.props.passwordHash;
  }

  public get firstName(): string {
    return this.props.firstName;
  }

  public get bio(): string | undefined {
    return this.props.bio;
  }

  public get verified(): boolean {
    return this.props.verified;
  }

  public getAge(): number {
    const today = new Date();
    const birthDate = this.props.dateOfBirth;
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }

    return age;
  }

  // Serialization
  public toJSON() {
    return {
      id: this.props.id,
      email: this.props.email.value,
      firstName: this.props.firstName,
      age: this.getAge(),
      bio: this.props.bio,
      verified: this.props.verified,
      createdAt: this.props.createdAt,
    };
  }

  private static generateId(): string {
    // UUID generation
    return crypto.randomUUID();
  }
}
```

**Value Object Example:**

```typescript
// src/domain/value-objects/Email.ts
import { DomainError } from '../errors/DomainError';

export class Email {
  private readonly _value: string;

  private constructor(value: string) {
    this._value = value;
  }

  public static create(email: string): Email {
    if (!this.isValidEmail(email)) {
      throw new DomainError('Invalid email format');
    }

    return new Email(email.toLowerCase().trim());
  }

  public get value(): string {
    return this._value;
  }

  public isValid(): boolean {
    return Email.isValidEmail(this._value);
  }

  private static isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  // Value objects are compared by value, not reference
  public equals(other: Email): boolean {
    return this._value === other._value;
  }
}
```

**Repository Interface:**

```typescript
// src/domain/repositories/IUserRepository.ts
import { User } from '../entities/User';
import { Email } from '../value-objects/Email';

export interface IUserRepository {
  findById(id: string): Promise<User | null>;
  findByEmail(email: Email): Promise<User | null>;
  save(user: User): Promise<void>;
  update(user: User): Promise<void>;
  delete(id: string): Promise<void>;
}
```

---

### Application Layer

**Use Case Example:**

```typescript
// src/application/use-cases/CreateUser/CreateUserUseCase.ts
import { User } from '../../../domain/entities/User';
import { Email } from '../../../domain/value-objects/Email';
import { Password } from '../../../domain/value-objects/Password';
import { IUserRepository } from '../../../domain/repositories/IUserRepository';
import { IEmailService } from '../../services/IEmailService';
import { CreateUserDTO } from './CreateUserDTO';
import { ApplicationError } from '../../errors/ApplicationError';

export class CreateUserUseCase {
  constructor(
    private userRepository: IUserRepository,
    private emailService: IEmailService
  ) {}

  async execute(dto: CreateUserDTO): Promise<User> {
    // 1. Create value objects
    const email = Email.create(dto.email);

    // 2. Check if user already exists
    const existingUser = await this.userRepository.findByEmail(email);
    if (existingUser) {
      throw new ApplicationError('User with this email already exists', 409);
    }

    // 3. Hash password
    const passwordHash = await Password.hash(dto.password);

    // 4. Create user entity
    const user = User.create({
      email,
      passwordHash,
      firstName: dto.firstName,
      dateOfBirth: new Date(dto.dateOfBirth),
      gender: dto.gender,
      verified: false,
    });

    // 5. Save to repository
    await this.userRepository.save(user);

    // 6. Send verification email (async, don't wait)
    this.emailService.sendVerificationEmail(email.value, user.id).catch(error => {
      console.error('Failed to send verification email:', error);
    });

    return user;
  }
}
```

**DTO:**

```typescript
// src/application/use-cases/CreateUser/CreateUserDTO.ts
export interface CreateUserDTO {
  email: string;
  password: string;
  firstName: string;
  dateOfBirth: string; // ISO date string
  gender: 'male' | 'female' | 'other';
}
```

---

### Infrastructure Layer

**Repository Implementation:**

```typescript
// src/infrastructure/database/repositories/PrismaUserRepository.ts
import { PrismaClient } from '@prisma/client';
import { User } from '../../../domain/entities/User';
import { Email } from '../../../domain/value-objects/Email';
import { IUserRepository } from '../../../domain/repositories/IUserRepository';

export class PrismaUserRepository implements IUserRepository {
  constructor(private prisma: PrismaClient) {}

  async findById(id: string): Promise<User | null> {
    const userData = await this.prisma.user.findUnique({
      where: { id },
    });

    if (!userData) return null;

    return this.toDomain(userData);
  }

  async findByEmail(email: Email): Promise<User | null> {
    const userData = await this.prisma.user.findUnique({
      where: { email: email.value },
    });

    if (!userData) return null;

    return this.toDomain(userData);
  }

  async save(user: User): Promise<void> {
    await this.prisma.user.create({
      data: this.toPersistence(user),
    });
  }

  async update(user: User): Promise<void> {
    await this.prisma.user.update({
      where: { id: user.id },
      data: this.toPersistence(user),
    });
  }

  async delete(id: string): Promise<void> {
    await this.prisma.user.delete({
      where: { id },
    });
  }

  // Mapper: Database -> Domain
  private toDomain(data: any): User {
    return User.fromPersistence({
      id: data.id,
      email: Email.create(data.email),
      passwordHash: data.passwordHash,
      firstName: data.firstName,
      dateOfBirth: data.dateOfBirth,
      gender: data.gender,
      bio: data.bio,
      verified: data.verified,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
    });
  }

  // Mapper: Domain -> Database
  private toPersistence(user: User) {
    return {
      id: user.id,
      email: user.email.value,
      passwordHash: user.passwordHash,
      firstName: user.firstName,
      bio: user.bio,
      verified: user.verified,
    };
  }
}
```

---

### Presentation Layer

**Controller:**

```typescript
// src/presentation/http/controllers/UserController.ts
import { Request, Response, NextFunction } from 'express';
import { CreateUserUseCase } from '../../../application/use-cases/CreateUser/CreateUserUseCase';
import { CreateUserDTO } from '../../../application/use-cases/CreateUser/CreateUserDTO';

export class UserController {
  constructor(private createUserUseCase: CreateUserUseCase) {}

  register = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const dto: CreateUserDTO = req.body;

      const user = await this.createUserUseCase.execute(dto);

      res.status(201).json({
        user: user.toJSON(),
      });
    } catch (error) {
      next(error); // Pass to error handling middleware
    }
  };
}
```

**Routes:**

```typescript
// src/presentation/http/routes/userRoutes.ts
import { Router } from 'express';
import { UserController } from '../controllers/UserController';
import { validateRequest } from '../middleware/validationMiddleware';
import { createUserSchema } from '../validators/UserValidator';

export function createUserRoutes(userController: UserController): Router {
  const router = Router();

  router.post(
    '/register',
    validateRequest(createUserSchema),
    userController.register
  );

  return router;
}
```

---

## Domain-Driven Design

### Aggregates

**Match Aggregate:**

```typescript
// src/domain/aggregates/Match.ts
import { DomainError } from '../errors/DomainError';
import { Message } from '../entities/Message';

export class Match {
  private _id: string;
  private _userAId: string;
  private _userBId: string;
  private _messages: Message[] = [];
  private _matchedAt: Date;
  private _unmatchedAt?: Date;

  private constructor(
    id: string,
    userAId: string,
    userBId: string,
    matchedAt: Date,
    messages: Message[] = []
  ) {
    this._id = id;
    this._userAId = userAId;
    this._userBId = userBId;
    this._matchedAt = matchedAt;
    this._messages = messages;

    this.ensureUsersAreDifferent();
  }

  public static create(userAId: string, userBId: string): Match {
    return new Match(
      crypto.randomUUID(),
      userAId,
      userBId,
      new Date()
    );
  }

  public static fromPersistence(
    id: string,
    userAId: string,
    userBId: string,
    matchedAt: Date,
    unmatchedAt?: Date,
    messages: Message[] = []
  ): Match {
    const match = new Match(id, userAId, userBId, matchedAt, messages);
    match._unmatchedAt = unmatchedAt;
    return match;
  }

  // Business logic
  public sendMessage(senderId: string, content: string): Message {
    if (!this.isUserInMatch(senderId)) {
      throw new DomainError('User is not part of this match');
    }

    if (this.isUnmatched()) {
      throw new DomainError('Cannot send message to unmatched users');
    }

    const message = Message.create(this._id, senderId, content);
    this._messages.push(message);

    return message;
  }

  public unmatch(): void {
    if (this.isUnmatched()) {
      throw new DomainError('Match is already unmatched');
    }

    this._unmatchedAt = new Date();
  }

  // Query methods
  public isUserInMatch(userId: string): boolean {
    return userId === this._userAId || userId === this._userBId;
  }

  public getOtherUser(userId: string): string {
    if (!this.isUserInMatch(userId)) {
      throw new DomainError('User is not part of this match');
    }

    return userId === this._userAId ? this._userBId : this._userAId;
  }

  public isUnmatched(): boolean {
    return this._unmatchedAt !== undefined;
  }

  private ensureUsersAreDifferent(): void {
    if (this._userAId === this._userBId) {
      throw new DomainError('Cannot match user with themselves');
    }
  }

  // Getters
  get id(): string {
    return this._id;
  }

  get userAId(): string {
    return this._userAId;
  }

  get userBId(): string {
    return this._userBId;
  }

  get messages(): readonly Message[] {
    return this._messages;
  }

  get matchedAt(): Date {
    return this._matchedAt;
  }

  get unmatchedAt(): Date | undefined {
    return this._unmatchedAt;
  }
}
```

---

## Dependency Injection

### Container Setup

```typescript
// src/di/container.ts
import { Container } from 'inversify';
import { PrismaClient } from '@prisma/client';
import Redis from 'ioredis';

// Symbols for dependency injection
export const TYPES = {
  // Infrastructure
  PrismaClient: Symbol.for('PrismaClient'),
  RedisClient: Symbol.for('RedisClient'),

  // Repositories
  IUserRepository: Symbol.for('IUserRepository'),
  IMatchRepository: Symbol.for('IMatchRepository'),

  // Services
  IEmailService: Symbol.for('IEmailService'),
  IStorageService: Symbol.for('IStorageService'),

  // Use Cases
  CreateUserUseCase: Symbol.for('CreateUserUseCase'),
  UpdateProfileUseCase: Symbol.for('UpdateProfileUseCase'),

  // Controllers
  UserController: Symbol.for('UserController'),
  ProfileController: Symbol.for('ProfileController'),
};

const container = new Container();

// Bind infrastructure
container.bind(TYPES.PrismaClient).toConstantValue(new PrismaClient());
container.bind(TYPES.RedisClient).toConstantValue(new Redis());

// Bind repositories
container.bind(TYPES.IUserRepository).to(PrismaUserRepository);
container.bind(TYPES.IMatchRepository).to(PrismaMatchRepository);

// Bind services
container.bind(TYPES.IEmailService).to(SendGridEmailService);
container.bind(TYPES.IStorageService).to(S3StorageService);

// Bind use cases
container.bind(TYPES.CreateUserUseCase).to(CreateUserUseCase);
container.bind(TYPES.UpdateProfileUseCase).to(UpdateProfileUseCase);

// Bind controllers
container.bind(TYPES.UserController).to(UserController);
container.bind(TYPES.ProfileController).to(ProfileController);

export { container };
```

**Injectable Controller:**

```typescript
// src/presentation/http/controllers/UserController.ts
import { injectable, inject } from 'inversify';
import { TYPES } from '../../../di/container';
import { CreateUserUseCase } from '../../../application/use-cases/CreateUser/CreateUserUseCase';

@injectable()
export class UserController {
  constructor(
    @inject(TYPES.CreateUserUseCase)
    private createUserUseCase: CreateUserUseCase
  ) {}

  // ... methods
}
```

---

## Error Handling

### Custom Error Classes

```typescript
// src/shared/errors/AppError.ts
export abstract class AppError extends Error {
  public readonly statusCode: number;
  public readonly isOperational: boolean;

  constructor(message: string, statusCode: number, isOperational = true) {
    super(message);
    this.statusCode = statusCode;
    this.isOperational = isOperational;

    Error.captureStackTrace(this, this.constructor);
  }
}

export class ValidationError extends AppError {
  constructor(message: string) {
    super(message, 400);
  }
}

export class UnauthorizedError extends AppError {
  constructor(message: string = 'Unauthorized') {
    super(message, 401);
  }
}

export class ForbiddenError extends AppError {
  constructor(message: string = 'Forbidden') {
    super(message, 403);
  }
}

export class NotFoundError extends AppError {
  constructor(message: string = 'Resource not found') {
    super(message, 404);
  }
}

export class ConflictError extends AppError {
  constructor(message: string) {
    super(message, 409);
  }
}

export class InternalServerError extends AppError {
  constructor(message: string = 'Internal server error') {
    super(message, 500, false); // Not operational
  }
}
```

### Global Error Handler

```typescript
// src/presentation/http/middleware/errorMiddleware.ts
import { Request, Response, NextFunction } from 'express';
import { AppError } from '../../../shared/errors/AppError';
import { logger } from '../../../shared/utils/logger';

export function errorHandler(
  error: Error,
  req: Request,
  res: Response,
  next: NextFunction
): void {
  // Log error
  logger.error({
    message: error.message,
    stack: error.stack,
    path: req.path,
    method: req.method,
  });

  // Handle known errors
  if (error instanceof AppError) {
    res.status(error.statusCode).json({
      error: {
        message: error.message,
        statusCode: error.statusCode,
      },
    });
    return;
  }

  // Handle Prisma errors
  if (error.name === 'PrismaClientKnownRequestError') {
    const prismaError = error as any;

    if (prismaError.code === 'P2002') {
      res.status(409).json({
        error: {
          message: 'Resource already exists',
          statusCode: 409,
        },
      });
      return;
    }
  }

  // Handle unknown errors
  res.status(500).json({
    error: {
      message: 'Internal server error',
      statusCode: 500,
    },
  });
}
```

### Async Handler Wrapper

```typescript
// src/shared/utils/asyncHandler.ts
import { Request, Response, NextFunction, RequestHandler } from 'express';

export function asyncHandler(fn: RequestHandler): RequestHandler {
  return (req: Request, res: Response, next: NextFunction) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}

// Usage
router.post('/register', asyncHandler(async (req, res) => {
  const user = await createUserUseCase.execute(req.body);
  res.status(201).json({ user });
}));
```

---

## Logging Strategy

### Winston + Morgan Setup

```typescript
// src/shared/utils/logger.ts
import winston from 'winston';

const logFormat = winston.format.combine(
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  winston.format.errors({ stack: true }),
  winston.format.splat(),
  winston.format.json()
);

export const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: logFormat,
  defaultMeta: { service: 'user-service' },
  transports: [
    // Console output
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.colorize(),
        winston.format.simple()
      ),
    }),
    // Error logs
    new winston.transports.File({
      filename: 'logs/error.log',
      level: 'error',
    }),
    // Combined logs
    new winston.transports.File({
      filename: 'logs/combined.log',
    }),
  ],
});

// Production: Send logs to external service
if (process.env.NODE_ENV === 'production') {
  // Add CloudWatch, Loggly, or other transport
}
```

**Morgan Integration:**

```typescript
// src/presentation/http/middleware/loggingMiddleware.ts
import morgan from 'morgan';
import { logger } from '../../../shared/utils/logger';

// Create stream for Morgan
const stream = {
  write: (message: string) => {
    logger.info(message.trim());
  },
};

// Morgan middleware
export const httpLogger = morgan(
  ':method :url :status :res[content-length] - :response-time ms',
  { stream }
);
```

---

## Testing Patterns

### Unit Tests (Jest)

```typescript
// tests/unit/domain/entities/User.test.ts
import { User } from '../../../../src/domain/entities/User';
import { Email } from '../../../../src/domain/value-objects/Email';
import { DomainError } from '../../../../src/domain/errors/DomainError';

describe('User Entity', () => {
  describe('create', () => {
    it('should create a valid user', () => {
      const user = User.create({
        email: Email.create('test@example.com'),
        passwordHash: 'hashed_password',
        firstName: 'John',
        dateOfBirth: new Date('1990-01-01'),
        gender: 'male',
        verified: false,
      });

      expect(user.firstName).toBe('John');
      expect(user.verified).toBe(false);
    });

    it('should throw error for underage user', () => {
      expect(() => {
        User.create({
          email: Email.create('test@example.com'),
          passwordHash: 'hashed_password',
          firstName: 'John',
          dateOfBirth: new Date('2010-01-01'), // Only 14 years old
          gender: 'male',
          verified: false,
        });
      }).toThrow(DomainError);
    });
  });

  describe('updateProfile', () => {
    it('should update user profile', () => {
      const user = User.create({
        email: Email.create('test@example.com'),
        passwordHash: 'hashed_password',
        firstName: 'John',
        dateOfBirth: new Date('1990-01-01'),
        gender: 'male',
        verified: false,
      });

      user.updateProfile('Jane', 'New bio');

      expect(user.firstName).toBe('Jane');
      expect(user.bio).toBe('New bio');
    });
  });
});
```

### Integration Tests

```typescript
// tests/integration/use-cases/CreateUser.test.ts
import { CreateUserUseCase } from '../../../src/application/use-cases/CreateUser/CreateUserUseCase';
import { PrismaUserRepository } from '../../../src/infrastructure/database/repositories/PrismaUserRepository';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();
const userRepository = new PrismaUserRepository(prisma);
const emailService = new MockEmailService();

describe('CreateUserUseCase', () => {
  beforeAll(async () => {
    await prisma.$connect();
  });

  afterAll(async () => {
    await prisma.user.deleteMany();
    await prisma.$disconnect();
  });

  it('should create a new user', async () => {
    const useCase = new CreateUserUseCase(userRepository, emailService);

    const dto = {
      email: 'test@example.com',
      password: 'SecurePass123!',
      firstName: 'John',
      dateOfBirth: '1990-01-01',
      gender: 'male' as const,
    };

    const user = await useCase.execute(dto);

    expect(user.email.value).toBe('test@example.com');
    expect(user.firstName).toBe('John');

    // Verify user was saved to database
    const savedUser = await prisma.user.findUnique({
      where: { email: 'test@example.com' },
    });

    expect(savedUser).toBeDefined();
  });
});
```

### API Integration Tests (Supertest)

```typescript
// tests/integration/api/user.test.ts
import request from 'supertest';
import { app } from '../../../src/app';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

describe('User API', () => {
  beforeAll(async () => {
    await prisma.$connect();
  });

  afterAll(async () => {
    await prisma.user.deleteMany();
    await prisma.$disconnect();
  });

  describe('POST /api/v1/users/register', () => {
    it('should register a new user', async () => {
      const response = await request(app)
        .post('/api/v1/users/register')
        .send({
          email: 'test@example.com',
          password: 'SecurePass123!',
          firstName: 'John',
          dateOfBirth: '1990-01-01',
          gender: 'male',
        })
        .expect(201);

      expect(response.body.user).toMatchObject({
        email: 'test@example.com',
        firstName: 'John',
      });
    });

    it('should return 400 for invalid email', async () => {
      const response = await request(app)
        .post('/api/v1/users/register')
        .send({
          email: 'invalid-email',
          password: 'SecurePass123!',
          firstName: 'John',
          dateOfBirth: '1990-01-01',
          gender: 'male',
        })
        .expect(400);

      expect(response.body.error).toBeDefined();
    });
  });
});
```

---

## API Versioning

### URL-Based Versioning

```typescript
// src/presentation/http/routes/index.ts
import { Router } from 'express';
import { createUserRoutes } from './v1/userRoutes';
import { createUserRoutesV2 } from './v2/userRoutes';

export function createRoutes(container: Container): Router {
  const router = Router();

  // Version 1
  router.use('/api/v1/users', createUserRoutes(container));

  // Version 2
  router.use('/api/v2/users', createUserRoutesV2(container));

  return router;
}
```

### Header-Based Versioning (Advanced)

```typescript
// src/presentation/http/middleware/versionMiddleware.ts
import { Request, Response, NextFunction } from 'express';

export function extractApiVersion(req: Request, res: Response, next: NextFunction) {
  const version = req.headers['x-api-version'] || req.query.version || '1';
  req.apiVersion = parseInt(version as string, 10);
  next();
}

// Usage in routes
app.use(extractApiVersion);

router.get('/users', (req, res) => {
  if (req.apiVersion === 2) {
    // Handle v2 logic
  } else {
    // Handle v1 logic
  }
});
```

---

## Summary

This architecture provides:

✅ **Separation of Concerns**: Clear boundaries between layers
✅ **Testability**: Easy to unit test business logic
✅ **Maintainability**: Each component has a single responsibility
✅ **Scalability**: Independent services can be scaled separately
✅ **Flexibility**: Easy to swap implementations (e.g., change database)

**Next**: See mobile architecture patterns and performance optimization guides.
