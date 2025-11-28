# Implementation Guide - JPA Entity Fixes

This guide provides exact code changes needed to fix all critical and high-impact issues.

---

## FIX #1: Create AuditLog.java

**Location:** `/home/user/POC_Dating/backend/user-service/src/main/java/com/dating/user/model/AuditLog.java`

**Status:** NEW FILE

```java
package com.dating.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * AuditLog entity - records all entity changes for audit trail.
 * Maps to the 'audit_logs' table in the database.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "entity_type", length = 100)
    private String entityType;
    
    @Column(name = "entity_id")
    private UUID entityId;
    
    @Column(name = "action", length = 50)
    private String action;  // CREATE, UPDATE, DELETE
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, Object> changes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

---

## FIX #2: Update Message.java - Line 19-24

**Location:** `/home/user/POC_Dating/backend/chat-service/src/main/java/com/dating/chat/model/Message.java`

**Current Code (Lines 18-24):**
```java
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_match_id", columnList = "match_id"),
    @Index(name = "idx_messages_sender", columnList = "sender_id"),
    @Index(name = "idx_messages_status", columnList = "status"),
    @Index(name = "idx_messages_created", columnList = "created_at")
})
```

**New Code (Lines 18-26):**
```java
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_match_id", columnList = "match_id"),
    @Index(name = "idx_messages_sender_id", columnList = "sender_id"),
    @Index(name = "idx_messages_status", columnList = "status"),
    @Index(name = "idx_messages_created_at", columnList = "created_at"),
    @Index(name = "idx_messages_match_created", columnList = "match_id, created_at")
})
```

**Changes:**
1. Rename: `idx_messages_sender` → `idx_messages_sender_id`
2. Rename: `idx_messages_created` → `idx_messages_created_at`
3. Add: `@Index(name = "idx_messages_match_created", columnList = "match_id, created_at")`

---

## FIX #3: Update Message.java - Line 57

**Location:** `/home/user/POC_Dating/backend/chat-service/src/main/java/com/dating/chat/model/Message.java`

**Current Code (Line 56-59):**
```java
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
```

**New Code (Line 56-59):**
```java
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
```

**Change:** `length = 50` → `length = 20`

---

## FIX #4: Update RefreshToken.java - Line 18-23

**Location:** `/home/user/POC_Dating/backend/user-service/src/main/java/com/dating/user/model/RefreshToken.java`

**Current Code (Lines 17-23):**
```java
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_tokens_revoked", columnList = "revoked")
})
```

**New Code (Lines 17-22):**
```java
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_tokens_revoked", columnList = "revoked")
})
```

**Change:** Remove line with `idx_refresh_tokens_token_hash`

---

## FIX #5: Create Flyway Migration

**Location:** `/home/user/POC_Dating/backend/user-service/src/main/resources/db/migration/V003__Add_Partial_Indexes.sql`

**Status:** NEW FILE

**Content:**
```sql
-- ==============================================
-- PARTIAL INDEXES (WHERE clause)
-- ==============================================
-- These cannot be expressed in JPA @Index annotations
-- and must be created separately

-- Partial index for messages status (only non-deleted messages)
CREATE INDEX IF NOT EXISTS idx_messages_status 
  ON messages(status) WHERE deleted_at IS NULL;

-- Partial index for refresh tokens (only active tokens)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_revoked 
  ON refresh_tokens(revoked) WHERE revoked = false;

-- ==============================================
-- INDEXES WITH DESC ORDERING
-- ==============================================
-- JPA @Index doesn't support sort direction specification
-- These ensure proper index ordering for queries

-- User profile creation queries (most recent first)
CREATE INDEX IF NOT EXISTS idx_users_created_at 
  ON users(created_at DESC);

-- Swipe feed queries (most recent first)
CREATE INDEX IF NOT EXISTS idx_swipes_created_at 
  ON swipes(created_at DESC);

-- Message history queries (most recent first)
CREATE INDEX IF NOT EXISTS idx_messages_created_at 
  ON messages(created_at DESC);

-- Chat history by match and date (most recent first)
CREATE INDEX IF NOT EXISTS idx_messages_match_created 
  ON messages(match_id, created_at DESC);

-- Audit log queries (most recent first)
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at 
  ON audit_logs(created_at DESC);
```

**Note:** Check existing migration files to determine the correct version number:
- If V001, V002 already exist, use V003
- If V002 doesn't exist, use V002
- List migrations: `ls /home/user/POC_Dating/backend/user-service/src/main/resources/db/migration/`

---

## VERIFICATION COMMANDS

After implementing all fixes, run these commands to verify:

```bash
# 1. Navigate to backend directory
cd /home/user/POC_Dating/backend

# 2. Clean and compile (catches syntax errors immediately)
mvn clean compile -q

# 3. Check that all entities are recognized
mvn clean install -q -DskipTests

# 4. Run unit tests
mvn test -q

# 5. Check specific entity mappings (from user-service directory)
cd user-service
mvn spring-boot:run -Dspring-boot.run.arguments="--debug" 2>&1 | \
  grep -E "(Generating|Table|Index|Entity)" | head -50

# 6. Database verification (after docker-compose up)
docker exec -it dating_postgres psql -U dating_user -d dating_db << SQL
-- Verify AuditLog table exists and has proper structure
\d audit_logs
-- Check messages table structure
\d messages
-- List all indexes
\di | grep -E "(messages|audit_logs|refresh_tokens)"
-- Check specific index definitions
SELECT indexname, indexdef FROM pg_indexes 
  WHERE tablename IN ('messages', 'audit_logs', 'refresh_tokens')
  ORDER BY tablename, indexname;
SQL
```

---

## COMMIT MESSAGE SUGGESTIONS

After making each fix, create appropriate commits:

```bash
# Fix 1: Create AuditLog entity
git add backend/user-service/src/main/java/com/dating/user/model/AuditLog.java
git commit -m "feat: add AuditLog entity for audit trail functionality"

# Fix 2-3: Update Message entity
git add backend/chat-service/src/main/java/com/dating/chat/model/Message.java
git commit -m "fix: correct Message entity index definitions and column constraints"

# Fix 4: Update RefreshToken entity
git add backend/user-service/src/main/java/com/dating/user/model/RefreshToken.java
git commit -m "fix: remove redundant RefreshToken index on token_hash"

# Fix 5: Create Flyway migration
git add backend/user-service/src/main/resources/db/migration/V003__Add_Partial_Indexes.sql
git commit -m "fix: add database migration for partial indexes and DESC ordering"
```

---

## ROLLBACK PROCEDURE

If any issue arises, use these commands to rollback:

```bash
# Undo the last commit (but keep changes in working directory)
git reset --soft HEAD~1

# View what changed
git diff --cached

# Discard changes if needed
git reset --hard HEAD
```

---

## TESTING CHECKLIST

After implementation, verify:

- [ ] All entities compile without errors
- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] AuditLog entity can be instantiated
- [ ] Message entity saves and loads correctly
- [ ] RefreshToken entity saves and loads correctly
- [ ] Database schema matches entity definitions
- [ ] All indexes exist in database
- [ ] Flyway migrations execute successfully
- [ ] No duplicate indexes on token_hash

---

## PERFORMANCE VERIFICATION

After deploying fixes, monitor:

1. **Message Queries Performance:**
   ```sql
   EXPLAIN ANALYZE SELECT * FROM messages 
   WHERE match_id = '...' 
   ORDER BY created_at DESC LIMIT 20;
   ```

2. **Index Usage:**
   ```sql
   SELECT schemaname, tablename, indexname, idx_scan
   FROM pg_stat_user_indexes
   WHERE tablename IN ('messages', 'audit_logs', 'refresh_tokens')
   ORDER BY idx_scan DESC;
   ```

3. **Missing Indexes:**
   ```sql
   SELECT query, calls, total_time
   FROM pg_stat_statements
   WHERE query LIKE '%messages%'
   ORDER BY total_time DESC;
   ```

---

**Last Updated:** November 18, 2025
**Version:** 1.0
**Status:** Ready for Implementation

For detailed analysis and context, refer to: `JPA_ENTITY_AUDIT_REPORT.md`
