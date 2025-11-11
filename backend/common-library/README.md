# Common Library

## Purpose

Shared code and utilities across all microservices.

## Contents (To Be Created)

### Entities
- `User`: User profile information
- `Match`: Match records between users
- `Message`: Chat messages
- `Preference`: User preferences for matching

### DTOs (Data Transfer Objects)
- Request DTOs: API request payloads
- Response DTOs: API response payloads
- Mapper classes: Convert entities ↔ DTOs

### Exceptions
- `DatingAppException`: Base exception
- `UserNotFoundException`
- `UnauthorizedException`
- `ValidationException`

### Constants & Enums
- `UserStatus`: ACTIVE, SUSPENDED, DELETED
- `MatchStatus`: PENDING, ACCEPTED, REJECTED
- `MessageStatus`: SENT, DELIVERED, READ

### Utilities
- Validators: Email, phone, age validation
- Converters: Date/time converters
- Mappers: Entity to DTO mapping helpers

## Design Rationale

### Why a Common Library?
- **DRY Principle**: Shared models defined once, used everywhere
- **Consistency**: All services use same entity definitions
- **Maintainability**: Single source of truth for data structures
- **Reduced Coupling**: Services don't need to know about each other's internal logic

### What NOT to Put Here
- ❌ Business logic (goes in individual service modules)
- ❌ Repository interfaces (each service defines own)
- ❌ Service classes (business-specific)
- ❌ Controllers (service-specific REST endpoints)

### Dependencies
- **Minimal**: Only JPA, Lombok, Jackson (JSON)
- **No Spring Web**: This is not a web module
- **No Database**: No active database dependencies, only JPA annotations

## Usage by Services

Each microservice includes this as a dependency:

```xml
<dependency>
    <groupId>com.dating</groupId>
    <artifactId>common-library</artifactId>
</dependency>
```

Then imports classes:
```java
import com.dating.common.entity.User;
import com.dating.common.dto.UserResponse;
import com.dating.common.exception.UserNotFoundException;
```

## Future Enhancements

- Add MapStruct for advanced entity-DTO mapping
- Add validation constraints as reusable annotations
- Add utility functions for pagination, sorting
