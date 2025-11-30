# Critical Fixes Needed - JPA Entity Audit

Generated: November 18, 2025

## QUICK SUMMARY

- **3 Critical Issues** (must fix today)
- **4 High-Impact Issues** (fix this week)
- **4 Medium Issues** (fix within 2 weeks)
- **Overall Compliance:** 89%

---

## PRIORITY 1: FIX TODAY (5 minutes total)

### Issue 1.1: Missing AuditLog Entity

**File to Create:**
```
/home/user/POC_Dating/backend/user-service/src/main/java/com/dating/user/model/AuditLog.java
```

**Reason:** Table `audit_logs` exists in database but has NO JPA entity. Audit trail is non-functional.

**Template:**
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

**Time:** 5 minutes

---

### Issue 1.2: Message Status Column Length

**File to Edit:**
```
/home/user/POC_Dating/backend/chat-service/src/main/java/com/dating/chat/model/Message.java
```

**Line:** 57

**Change From:**
```java
@Column(nullable = false, length = 50)
private MessageStatus status = MessageStatus.SENT;
```

**Change To:**
```java
@Column(nullable = false, length = 20)
private MessageStatus status = MessageStatus.SENT;
```

**Reason:** Schema specifies VARCHAR(20), entity incorrectly uses length=50. This causes schema misalignment.

**Time:** 1 minute

---

### Issue 1.3: Missing Message Composite Index

**File to Edit:**
```
/home/user/POC_Dating/backend/chat-service/src/main/java/com/dating/chat/model/Message.java
```

**Line:** 19-24 (the @Table annotation)

**Change From:**
```java
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_match_id", columnList = "match_id"),
    @Index(name = "idx_messages_sender", columnList = "sender_id"),
    @Index(name = "idx_messages_status", columnList = "status"),
    @Index(name = "idx_messages_created", columnList = "created_at")
})
```

**Change To:**
```java
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_match_id", columnList = "match_id"),
    @Index(name = "idx_messages_sender_id", columnList = "sender_id"),
    @Index(name = "idx_messages_status", columnList = "status"),
    @Index(name = "idx_messages_created_at", columnList = "created_at"),
    @Index(name = "idx_messages_match_created", columnList = "match_id, created_at")
})
```

**Reason:** 
1. Composite index on (match_id, created_at) is missing - used for chat history queries
2. Index names should match schema: sender_id not sender, created_at not created

**Time:** 1 minute

---

## PRIORITY 2: FIX THIS WEEK (5 minutes)

### Issue 2.1: Redundant RefreshToken Index

**File to Edit:**
```
/home/user/POC_Dating/backend/user-service/src/main/java/com/dating/user/model/RefreshToken.java
```

**Line:** 20

**Change From:**
```java
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_tokens_revoked", columnList = "revoked")
})
```

**Change To:**
```java
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_tokens_revoked", columnList = "revoked")
})
```

**Reason:** Remove redundant `idx_refresh_tokens_token_hash`. The `token_hash` column is marked UNIQUE, which creates an implicit index in PostgreSQL.

**Time:** 1 minute

---

### Issue 2.2: Create Flyway Migration for Partial Indexes

**File to Create:**
```
/home/user/POC_Dating/backend/user-service/src/main/resources/db/migration/V003__Add_Partial_Indexes.sql
```

(Adjust version number if other migrations already exist)

**Content:**
```sql
-- Partial index for messages status (only non-deleted messages)
CREATE INDEX idx_messages_status ON messages(status) WHERE deleted_at IS NULL;

-- Partial index for refresh tokens (only active tokens)
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked) WHERE revoked = false;

-- Indexes with DESC ordering
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_swipes_created_at ON swipes(created_at DESC);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_match_created ON messages(match_id, created_at DESC);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
```

**Reason:** JPA @Index doesn't support:
- WHERE clauses (partial indexes)
- DESC sort direction

These must be created separately via database migration.

**Time:** 2 minutes

---

## PRIORITY 3: FIX WITHIN 2 WEEKS (Optional but recommended)

### Issue 3.1: Add Validation Annotations

Add Bean Validation to enforce database CHECK constraints at the Java level:

**UserPreference.java:**
```java
@Range(min = 18, message = "Minimum age must be at least 18")
private Integer minAge = 18;

@Range(max = 150, message = "Maximum age cannot exceed 150")
private Integer maxAge = 99;
```

**Match.java:**
```java
// Note: MatchScore has score validation
@Positive(message = "Score must be positive")
@DecimalMax("100", message = "Score cannot exceed 100")
private BigDecimal score;
```

**Effort:** 30 minutes

---

## VERIFICATION CHECKLIST

After making changes, verify:

```bash
# 1. Check that all files compile
mvn clean compile

# 2. Verify entity scanning
mvn spring-boot:run -Dspring-boot.run.arguments="--debug" | grep -i "entity\|table"

# 3. Check database schema alignment
docker exec -it dating_postgres psql -U dating_user -d dating_db -c "\d audit_logs"
docker exec -it dating_postgres psql -U dating_user -d dating_db -c "\d messages"

# 4. Verify indexes
docker exec -it dating_postgres psql -U dating_user -d dating_db -c "\di"

# 5. Run tests
mvn test
```

---

## SUMMARY OF CHANGES

| Entity | Changes | Severity | Time |
|--------|---------|----------|------|
| AuditLog (NEW) | Create new entity | CRITICAL | 5 min |
| Message | Fix length + fix index names + add composite index | CRITICAL | 1 min |
| RefreshToken | Remove redundant index | HIGH | 1 min |
| All | Create Flyway for partial/DESC indexes | HIGH | 2 min |
| Multiple | Add validation annotations (optional) | MEDIUM | 30 min |

**Total Blocking Time:** 9 minutes
**Total Optional Time:** 30 minutes

---

## DETAILED AUDIT REPORT

For complete analysis, see: `/home/user/POC_Dating/JPA_ENTITY_AUDIT_REPORT.md`

This document contains:
- Full schema vs entity comparison for each table
- Detailed impact analysis for every issue
- Code examples for each fix
- Recommendations for improvements

---

**Generated:** November 18, 2025
**Status:** Ready for implementation
**Questions?** Review detailed report for more context.
