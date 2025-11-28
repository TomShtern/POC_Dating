# COMPREHENSIVE SCHEMA AUDIT REPORT
## POC Dating Application Database Schema
**File:** `/home/user/POC_Dating/db/init/01-schema.sql`  
**Date:** 2025-11-18  
**Severity Breakdown:** CRITICAL (3) | HIGH (6) | MEDIUM (8) | LOW (5)

---

# SECTION 1: CRITICAL ISSUES (Must Fix - Blocks Production)

## ISSUE #1: CRITICAL - Missing Sender Validation in Messages Table

**Location:** `messages` table (lines 166-180)  
**Severity:** CRITICAL  
**Category:** Security + Data Integrity  

### Current State
```sql
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- ❌ NO VALIDATION
    content TEXT NOT NULL,
    ...
    CONSTRAINT valid_status CHECK (status IN ('SENT', 'DELIVERED', 'READ')),
    CONSTRAINT valid_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'GIF', 'AUDIO'))
);
```

### Problem
- `sender_id` only validates that user exists in `users` table
- **No constraint** that `sender_id` is either `user1_id` or `user2_id` in the associated `match`
- A user could theoretically send messages "as" another user in a match (privilege escalation)
- No data integrity guarantee

### Risk Examples
```
Scenario: User A and User B match
- Attacker C could send message: INSERT INTO messages (match_id, sender_id, content)
  VALUES (match_id_of_A_B, user_a_id, 'Hello B, buy this...')
- System shows User A sent message when it was User C
- Reputational damage + account compromise
```

### Recommended Fix
Add CHECK constraint with subquery validation:
```sql
CONSTRAINT sender_in_match CHECK (
    EXISTS (
        SELECT 1 FROM matches
        WHERE id = match_id
        AND (user1_id = sender_id OR user2_id = sender_id)
    )
)
```

**Alternative (Better):** Use trigger for more flexibility:
```sql
CREATE OR REPLACE FUNCTION validate_message_sender()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM matches
        WHERE id = NEW.match_id
        AND (user1_id = NEW.sender_id OR user2_id = NEW.sender_id)
    ) THEN
        RAISE EXCEPTION 'Sender is not part of this match';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_message_sender
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION validate_message_sender();
```

**SQL to Apply Fix:**
```sql
-- Drop existing triggers/constraints if needed
ALTER TABLE messages DROP CONSTRAINT IF EXISTS sender_in_match;

-- Add trigger to validate sender is part of match
CREATE OR REPLACE FUNCTION validate_message_sender()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM matches
        WHERE id = NEW.match_id
        AND (user1_id = NEW.sender_id OR user2_id = NEW.sender_id)
    ) THEN
        RAISE EXCEPTION 'Sender % is not part of match %', NEW.sender_id, NEW.match_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_message_sender
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION validate_message_sender();
```

---

## ISSUE #2: CRITICAL - Password Hash Insufficient Validation

**Location:** `users` table, column `password_hash` (line 32)  
**Severity:** CRITICAL  
**Category:** Security  

### Current State
```sql
password_hash VARCHAR(255) NOT NULL,  -- ❌ No length validation for bcrypt format
```

### Problem
- BCrypt hashes are exactly **60 characters** (including "$2a$", "$2b$", or "$2y$" prefix and salt)
- Truncated or invalid hashes could be stored
- No validation that hash actually conforms to bcrypt format
- VARCHAR(255) is too permissive - accepts any string

### Risk Examples
```
- Accidental storage of plaintext password: "myPassword123"
- Truncated hash from external system: "abc123"
- Invalid bcrypt format: "sha256:somehash"
- Database accepts all, causing authentication failures downstream
```

### Recommended Fix
```sql
ALTER TABLE users 
ADD CONSTRAINT password_hash_bcrypt_format CHECK (
    length(password_hash) >= 60 
    AND (
        password_hash LIKE '$2a$%' 
        OR password_hash LIKE '$2b$%' 
        OR password_hash LIKE '$2y$%'
    )
);
```

**SQL to Apply:**
```sql
ALTER TABLE users
ADD CONSTRAINT password_hash_bcrypt_format CHECK (
    length(password_hash) >= 60
    AND password_hash ~ '^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$'
);
```

**Better Approach (Prevent Issue):** Application layer
- Enforce bcrypt hashing in Java with Spring Security `BCryptPasswordEncoder`
- Never accept non-bcrypt hashes
- Add schema CHECK as safety net only

---

## ISSUE #3: CRITICAL - Match Ending Without Proper State

**Location:** `matches` table (lines 130-142)  
**Severity:** CRITICAL  
**Category:** Data Consistency  

### Current State
```sql
CREATE TABLE IF NOT EXISTS matches (
    ...
    status VARCHAR(20) DEFAULT 'ACTIVE',
    matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,  -- ❌ Can be NULL even if status = UNMATCHED/BLOCKED
    ended_by UUID REFERENCES users(id) ON DELETE SET NULL,
    ...
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'UNMATCHED', 'BLOCKED')),
    ...
);
```

### Problem
- If `status = 'UNMATCHED'` or `'BLOCKED'`, `ended_at` SHOULD be NOT NULL
- Current schema allows: `status = 'UNMATCHED'` with `ended_at = NULL`
- No audit trail of when match ended
- Reports/analytics become inaccurate

### Risk Example
```
- Match created: 2025-01-01 12:00
- User unmatches on: 2025-01-05 14:30
- INSERT INTO matches: status='UNMATCHED', ended_at=NULL  ❌
- Query: "When was match ended?" → Returns NULL
- Analytics report: "This match is still active" ❌
```

### Recommended Fix
```sql
ALTER TABLE matches
ADD CONSTRAINT end_state_consistency CHECK (
    (status = 'ACTIVE' AND ended_at IS NULL)
    OR
    (status IN ('UNMATCHED', 'BLOCKED') AND ended_at IS NOT NULL)
);
```

**SQL to Apply:**
```sql
ALTER TABLE matches
ADD CONSTRAINT end_state_consistency CHECK (
    (status = 'ACTIVE' AND ended_at IS NULL AND ended_by IS NULL)
    OR
    (status IN ('UNMATCHED', 'BLOCKED') AND ended_at IS NOT NULL)
);
```

---

# SECTION 2: HIGH PRIORITY ISSUES (Should Fix - Production Quality)

## ISSUE #4: HIGH - Location Data Type Precision Loss

**Location:** `users` table, columns `location_lat` and `location_lng` (lines 39-40)  
**Severity:** HIGH  
**Category:** Data Quality/Precision  

### Current State
```sql
location_lat DECIMAL(10, 8),   -- ❌ Precision issue
location_lng DECIMAL(11, 8),   -- ⚠️ Precision issue
```

### Problem
**Latitude Range:** -90° to +90°
- Requires 3 digits before decimal (±90)
- 8 digits after decimal = 11.16m precision ✓
- **DECIMAL(10, 8)** = max 10 total digits, 8 after decimal = only 2 digits before decimal
- Maximum value possible: 99.99999999 (OK for latitude but no margin)
- **Actual issue:** Latitude values like -90.12345678 would be rejected ❌

**Longitude Range:** -180° to +180°
- Requires 4 digits before decimal (±180)
- 8 digits after decimal = DECIMAL(12, 8) or DECIMAL(13, 8)
- **DECIMAL(11, 8)** = only 3 digits before decimal = insufficient ❌
- Longitude value -180.0 is valid but structure is suboptimal

### Risk Example
```
User location: 85.12345678, -170.98765432
INSERT fails on latitude (too many digits before decimal)
Geographic features don't work
Matching by distance unavailable
```

### Recommended Fix
```sql
-- Replace:
location_lat DECIMAL(10, 8),
location_lng DECIMAL(11, 8),

-- With:
location_lat DECIMAL(11, 8),  -- 3 digits + 8 decimals = ±90.12345678 with margin
location_lng DECIMAL(12, 8),  -- 4 digits + 8 decimals = ±180.12345678 with margin
```

**SQL to Apply:**
```sql
ALTER TABLE users
ALTER COLUMN location_lat TYPE DECIMAL(11, 8),
ALTER COLUMN location_lng TYPE DECIMAL(12, 8);
```

---

## ISSUE #5: HIGH - Missing Verification Code Uniqueness

**Location:** `verification_codes` table (lines 270-281)  
**Severity:** HIGH  
**Category:** Security/Logic  

### Current State
```sql
CREATE TABLE IF NOT EXISTS verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,  -- ❌ No uniqueness constraint
    type VARCHAR(20) NOT NULL,   -- Email, Phone, Password Reset
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    attempts INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ...
);
```

### Problem
- No constraint ensuring a user can have only one **active** code per type
- A user could have multiple valid codes for same type simultaneously
- Verification logic becomes ambiguous:
  - Which code do we check against?
  - User's application might accept first/last/any code
- Allows unlimited code generation attacks

### Risk Example
```
User A requests password reset
System generates code: 123456 (expires in 10 min)
User A requests password reset again
System generates code: 789012 (expires in 10 min)
System now has 2 valid codes for user A password reset
Attacker could try both codes
Or user could reset with either code
```

### Recommended Fix (Approach 1: Partial Index)
```sql
ALTER TABLE verification_codes
ADD CONSTRAINT unique_active_code_per_type UNIQUE (user_id, type)
WHERE used_at IS NULL AND expires_at > CURRENT_TIMESTAMP;
```

**Problem with above:** PostgreSQL doesn't support UNIQUE with WHERE clause directly in table definition.

**Better Approach: Use Trigger**
```sql
CREATE OR REPLACE FUNCTION ensure_single_active_code()
RETURNS TRIGGER AS $$
BEGIN
    -- Invalidate previous active codes of same type
    UPDATE verification_codes
    SET used_at = CURRENT_TIMESTAMP
    WHERE user_id = NEW.user_id
    AND type = NEW.type
    AND used_at IS NULL
    AND id != NEW.id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_single_active_code
    BEFORE INSERT ON verification_codes
    FOR EACH ROW
    EXECUTE FUNCTION ensure_single_active_code();
```

**SQL to Apply:**
```sql
CREATE OR REPLACE FUNCTION ensure_single_active_code()
RETURNS TRIGGER AS $$
BEGIN
    -- Invalidate all previous active codes of same type for this user
    UPDATE verification_codes
    SET used_at = CURRENT_TIMESTAMP
    WHERE user_id = NEW.user_id
    AND type = NEW.type
    AND used_at IS NULL
    AND id != NEW.id
    AND expires_at > CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ensure_single_active_code
    BEFORE INSERT ON verification_codes
    FOR EACH ROW
    EXECUTE FUNCTION ensure_single_active_code();
```

---

## ISSUE #6: HIGH - Missing NOT NULL Constraints in Match Scores

**Location:** `match_scores` table (lines 151-157)  
**Severity:** HIGH  
**Category:** Data Integrity  

### Current State
```sql
CREATE TABLE IF NOT EXISTS match_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    score NUMERIC(5, 2) CHECK (score >= 0 AND score <= 100),  -- ❌ Can be NULL
    factors JSONB,  -- ❌ Can be NULL
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Problem
- `score` is the primary purpose of this table but can be NULL
- `factors` (breakdown) can be NULL
- A record with NULL score is meaningless
- Queries like `SELECT * FROM match_scores WHERE score > 75` miss NULL records
- Application assumes `score` exists

### Recommended Fix
```sql
ALTER TABLE match_scores
ALTER COLUMN score SET NOT NULL,
ALTER COLUMN factors SET NOT NULL DEFAULT '{}';
```

**SQL to Apply:**
```sql
-- First, set defaults for existing NULLs
UPDATE match_scores SET score = 50 WHERE score IS NULL;
UPDATE match_scores SET factors = '{}' WHERE factors IS NULL;

-- Then add NOT NULL constraints
ALTER TABLE match_scores
ALTER COLUMN score SET NOT NULL,
ALTER COLUMN factors SET NOT NULL DEFAULT '{}';
```

---

## ISSUE #7: HIGH - Missing Orphan Prevention on Updated Messages

**Location:** `messages` table (lines 166-180)  
**Severity:** HIGH  
**Category:** Data Integrity  

### Current State
```sql
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- ❌ Deletes message record
    ...
);
```

### Problem
- When a user deletes their account, all their messages are deleted
- Chat history is lost even though other user may want to keep it
- Typical pattern: SET NULL sender_id instead of CASCADE
- Message content preserved but authorship becomes anonymous
- Better for user experience and data retention

### Recommended Fix
```sql
-- Keep message but anonymize sender
ALTER TABLE messages
DROP CONSTRAINT messages_sender_id_fkey,
ADD CONSTRAINT messages_sender_id_fkey 
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL;
```

**SQL to Apply:**
```sql
ALTER TABLE messages
DROP CONSTRAINT IF EXISTS messages_sender_id_fkey,
ADD CONSTRAINT messages_sender_id_fkey 
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL;
```

---

## ISSUE #8: HIGH - Bio Field Lacks Size Limit

**Location:** `users` table, column `bio` (line 37)  
**Severity:** HIGH  
**Category:** Security/UX  

### Current State
```sql
bio TEXT,  -- ❌ No size limit
```

### Problem
- TEXT allows unlimited length (up to 1GB in PostgreSQL)
- No constraint on bio length
- Users could paste entire books
- Bad UX, performance issue, waste storage
- Frontend may expect max length (e.g., 5000 chars)

### Risk Example
```
Attacker: INSERT INTO users (bio) VALUES (repeat('A', 100000000))
Result: 100MB bio field, database bloat
```

### Recommended Fix
```sql
ALTER TABLE users
ADD CONSTRAINT bio_max_length CHECK (length(bio) <= 5000);
```

**SQL to Apply:**
```sql
-- First, truncate any bios over 5000 characters
UPDATE users SET bio = substring(bio FROM 1 FOR 5000) WHERE length(bio) > 5000;

-- Then add constraint
ALTER TABLE users
ADD CONSTRAINT bio_max_length CHECK (length(bio) <= 5000);
```

---

## ISSUE #9: HIGH - User Interests Array Lacks Size Limit

**Location:** `user_preferences` table, column `interests` (line 75)  
**Severity:** HIGH  
**Category:** Performance/Security  

### Current State
```sql
interests TEXT[],  -- ❌ No size limit on array
```

### Problem
- TEXT[] allows unlimited array size
- User could add 10,000 interests
- Array queries become slow
- Bad for index performance
- No business reason for unlimited interests

### Recommended Fix
```sql
ALTER TABLE user_preferences
ADD CONSTRAINT interests_max_count CHECK (array_length(interests, 1) <= 50);
```

**SQL to Apply:**
```sql
-- First, truncate any interests arrays over 50 items
UPDATE user_preferences 
SET interests = interests[1:50] 
WHERE array_length(interests, 1) > 50;

-- Then add constraint
ALTER TABLE user_preferences
ADD CONSTRAINT interests_max_count CHECK (array_length(interests, 1) <= 50);
```

---

# SECTION 3: MEDIUM PRIORITY ISSUES (Improve Quality)

## ISSUE #10: MEDIUM - Refresh Tokens Can Have Inconsistent Revoke State

**Location:** `refresh_tokens` table (lines 188-198)  
**Severity:** MEDIUM  
**Category:** Logic Consistency  

### Current State
```sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
    ...
    revoked BOOLEAN DEFAULT false,
    revoked_at TIMESTAMP  -- ❌ Can be NULL even if revoked=true
);
```

### Problem
- If `revoked = true`, `revoked_at` should NOT be NULL
- Can have: `revoked = true`, `revoked_at = NULL`
- Audit trail unclear: when was token revoked?
- Application must infer revocation time from record creation time

### Recommended Fix
```sql
ALTER TABLE refresh_tokens
ADD CONSTRAINT revoke_state_consistency CHECK (
    (revoked = false AND revoked_at IS NULL)
    OR
    (revoked = true AND revoked_at IS NOT NULL)
);
```

**SQL to Apply:**
```sql
-- First, set revoked_at for existing revoked tokens
UPDATE refresh_tokens 
SET revoked_at = CURRENT_TIMESTAMP 
WHERE revoked = true AND revoked_at IS NULL;

-- Then add constraint
ALTER TABLE refresh_tokens
ADD CONSTRAINT revoke_state_consistency CHECK (
    (revoked = false AND revoked_at IS NULL)
    OR
    (revoked = true AND revoked_at IS NOT NULL)
);
```

---

## ISSUE #11: MEDIUM - Verification Code Attempts Not Limited

**Location:** `verification_codes` table, column `attempts` (line 277)  
**Severity:** MEDIUM  
**Category:** Security  

### Current State
```sql
attempts INT DEFAULT 0,  -- ❌ No maximum limit
```

### Problem
- Attacker could make unlimited attempts (1000000 tries)
- Brute force attack possible if codes are short (e.g., 6 digits = 1M combinations)
- No constraint preventing excessive attempts
- Application must enforce limit (fragile)

### Recommended Fix
```sql
ALTER TABLE verification_codes
ADD CONSTRAINT attempts_limit CHECK (attempts >= 0 AND attempts <= 5);
```

**SQL to Apply:**
```sql
ALTER TABLE verification_codes
ADD CONSTRAINT attempts_limit CHECK (attempts >= 0 AND attempts <= 5);

-- Add trigger to auto-invalidate after 5 failed attempts
CREATE OR REPLACE FUNCTION invalidate_code_on_max_attempts()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.attempts > 5 THEN
        RAISE EXCEPTION 'Verification code attempt limit exceeded';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_code_attempts
    BEFORE UPDATE ON verification_codes
    FOR EACH ROW
    EXECUTE FUNCTION invalidate_code_on_max_attempts();
```

---

## ISSUE #12: MEDIUM - Interaction History Missing Action Validation

**Location:** `interaction_history` table (lines 289-296)  
**Severity:** MEDIUM  
**Category:** Data Quality  

### Current State
```sql
CREATE TABLE IF NOT EXISTS interaction_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,  -- ❌ No CHECK constraint for valid actions
    target_id UUID REFERENCES users(id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Problem
- `action` can be any value (invalid actions accepted)
- No validation against enum of allowed actions
- Analytics queries unsure of action values
- Application must validate (fragile)

### Recommended Fix
```sql
ALTER TABLE interaction_history
ADD CONSTRAINT valid_action CHECK (action IN (
    'VIEW_PROFILE',
    'SEND_SWIPE',
    'SEND_SUPER_LIKE',
    'SEND_MESSAGE',
    'VIEW_MESSAGE',
    'REPORT_USER',
    'BLOCK_USER',
    'UNBLOCK_USER',
    'UNMATCH',
    'SHARE_PROFILE'
));
```

**SQL to Apply:**
```sql
ALTER TABLE interaction_history
ADD CONSTRAINT valid_action CHECK (action IN (
    'VIEW_PROFILE',
    'SEND_SWIPE', 
    'SEND_SUPER_LIKE',
    'SEND_MESSAGE',
    'VIEW_MESSAGE',
    'REPORT_USER',
    'BLOCK_USER',
    'UNBLOCK_USER',
    'UNMATCH',
    'SHARE_PROFILE'
));
```

---

## ISSUE #13: MEDIUM - Missing Column: Message Has No Update Tracking

**Location:** `messages` table (lines 166-180)  
**Severity:** MEDIUM  
**Category:** Auditability  

### Current State
```sql
CREATE TABLE IF NOT EXISTS messages (
    ...
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- ✓ Has created_at
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    deleted_at TIMESTAMP,
    -- ❌ MISSING: updated_at for message edits
);
```

### Problem
- Message TABLE has no `updated_at` column
- If message is edited/updated, no timestamp
- Can't track whether message content changed
- Other tables (users, photos) have `updated_at` but messages doesn't
- Inconsistent pattern

### Recommended Fix
```sql
ALTER TABLE messages
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add trigger to auto-update timestamp
CREATE OR REPLACE FUNCTION update_messages_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_messages_updated_at
    BEFORE UPDATE ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_messages_updated_at();
```

---

## ISSUE #14: MEDIUM - Missing Column: Swipes Have No Update Tracking

**Location:** `swipes` table (lines 112-122)  
**Severity:** MEDIUM  
**Category:** Auditability  

### Current State
```sql
CREATE TABLE IF NOT EXISTS swipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,  -- Can this change? Should there be updated_at?
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ❌ MISSING: updated_at if swipe action can change
    ...
);
```

### Problem
- Swipes are meant to be immutable (LIKE/PASS/SUPER_LIKE)
- But schema doesn't prevent modification
- No `updated_at` to track when swipe action changed
- Unlike photos/users, swipes lack audit timestamp

### Recommended Fix (if swipes are immutable)
```sql
-- Add assertion that swipe records cannot be modified
ALTER TABLE swipes
ADD CONSTRAINT immutable_swipe CHECK (true);  -- Application enforces via triggers

-- Or add updated_at for audit trail:
ALTER TABLE swipes
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

---

## ISSUE #15: MEDIUM - Notification State Machine Missing Constraint

**Location:** `notifications` table (lines 243-262)  
**Severity:** MEDIUM  
**Category:** Logic Consistency  

### Current State
```sql
CREATE TABLE IF NOT EXISTS notifications (
    ...
    is_read BOOLEAN DEFAULT false,
    is_sent BOOLEAN DEFAULT false,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    ...
    CONSTRAINT read_after_sent CHECK (is_read = false OR is_sent = true)  -- ✓ Good
);
```

### Problem
- Schema has good constraint: can't be read before sent ✓
- **BUT:** Missing: if `is_read = true`, then `read_at` must NOT be NULL
- Also missing: if `is_sent = true`, then `sent_at` must NOT be NULL
- Incomplete state machine validation

### Recommended Fix
```sql
ALTER TABLE notifications
ADD CONSTRAINT read_timestamp_consistency CHECK (
    (is_read = false AND read_at IS NULL)
    OR
    (is_read = true AND read_at IS NOT NULL)
),
ADD CONSTRAINT sent_timestamp_consistency CHECK (
    (is_sent = false AND sent_at IS NULL)
    OR
    (is_sent = true AND sent_at IS NOT NULL)
);
```

**SQL to Apply:**
```sql
-- First, fix any inconsistencies
UPDATE notifications SET read_at = CURRENT_TIMESTAMP WHERE is_read = true AND read_at IS NULL;
UPDATE notifications SET sent_at = CURRENT_TIMESTAMP WHERE is_sent = true AND sent_at IS NULL;

-- Then add constraints
ALTER TABLE notifications
ADD CONSTRAINT read_timestamp_consistency CHECK (
    (is_read = false AND read_at IS NULL)
    OR
    (is_read = true AND read_at IS NOT NULL)
),
ADD CONSTRAINT sent_timestamp_consistency CHECK (
    (is_sent = false AND sent_at IS NULL)
    OR
    (is_sent = true AND sent_at IS NOT NULL)
);
```

---

## ISSUE #16: MEDIUM - User Blocks Missing Expiration Capability

**Location:** `user_blocks` table (lines 226-235)  
**Severity:** MEDIUM  
**Category:** Feature Completeness  

### Current State
```sql
CREATE TABLE IF NOT EXISTS user_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ❌ MISSING: expires_at for temporary blocks
    ...
);
```

### Problem
- Blocks are permanent only (no expiration)
- Real dating apps allow temporary blocks (e.g., 7 days, 30 days)
- No way to track block duration
- If user blocks someone in anger, no way to unblock after cooling off

### Recommended Fix
```sql
ALTER TABLE user_blocks
ADD COLUMN expires_at TIMESTAMP,
ADD CONSTRAINT block_expiration_future CHECK (expires_at > created_at);

-- Add column for manual unblock
ALTER TABLE user_blocks
ADD COLUMN unblocked_at TIMESTAMP;
```

**SQL to Apply:**
```sql
ALTER TABLE user_blocks
ADD COLUMN expires_at TIMESTAMP,
ADD COLUMN unblocked_at TIMESTAMP,
ADD CONSTRAINT block_must_be_active CHECK (
    (unblocked_at IS NULL AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP))
);
```

---

# SECTION 4: LOW PRIORITY ISSUES (Polish & Best Practices)

## ISSUE #17: LOW - Verification Code Character Set Not Specified

**Location:** `verification_codes` table, column `code` (line 273)  
**Severity:** LOW  
**Category:** Security  

### Current State
```sql
code VARCHAR(10) NOT NULL,  -- ❌ No format constraint
```

### Problem
- Code can be any characters (including spaces, special chars, unicode)
- Should be numeric or alphanumeric only
- Typical email codes: 6-digit numbers (000000-999999)
- Typical 2FA codes: 6-digit numbers
- SMS codes: usually 6 digits

### Recommended Fix
```sql
ALTER TABLE verification_codes
ADD CONSTRAINT code_numeric_format CHECK (code ~ '^[0-9]{6}$');
```

**SQL to Apply:**
```sql
ALTER TABLE verification_codes
ADD CONSTRAINT code_numeric_format CHECK (code ~ '^[0-9]{6}$');
```

---

## ISSUE #18: LOW - Timestamp Timezone Handling

**Location:** All tables (created_at, updated_at, etc.)  
**Severity:** LOW  
**Category:** Best Practices  

### Current State
```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- ❌ No timezone
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
```

### Problem
- `TIMESTAMP` without timezone = ambiguous in multi-region setups
- If database is in UTC but UI displays different timezone, confusion
- Best practice: use `TIMESTAMPTZ` for distributed systems
- Not critical for single-region POC

### Recommended Fix (Optional)
```sql
-- Convert all timestamps to TIMESTAMPTZ
ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE users ALTER COLUMN updated_at TYPE TIMESTAMPTZ;
-- ... repeat for all tables
```

**SQL to Apply:**
```sql
-- Example for users table (repeat for all tables)
ALTER TABLE users
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
ALTER COLUMN last_active TYPE TIMESTAMPTZ USING last_active AT TIME ZONE 'UTC',
ALTER COLUMN last_login TYPE TIMESTAMPTZ USING last_login AT TIME ZONE 'UTC';
```

---

## ISSUE #19: LOW - Missing Audit Log Entity Type Validation

**Location:** `audit_logs` table (lines 329-338)  
**Severity:** LOW  
**Category:** Data Quality  

### Current State
```sql
CREATE TABLE IF NOT EXISTS audit_logs (
    ...
    entity_type VARCHAR(100),  -- ❌ No constraint for valid types
    action VARCHAR(50),         -- ❌ No constraint for valid actions
    ...
);
```

### Problem
- `entity_type` could be any string (USER, users, Users, etc.)
- `action` could be any string (create, CREATE, insert, INSERT)
- Inconsistent values make analytics/queries harder
- Should enforce enum of allowed values

### Recommended Fix
```sql
ALTER TABLE audit_logs
ADD CONSTRAINT valid_entity_type CHECK (entity_type IN (
    'USER', 'MATCH', 'MESSAGE', 'SWIPE', 'PHOTO', 'REPORT', 'BLOCK'
)),
ADD CONSTRAINT valid_action CHECK (action IN (
    'CREATE', 'READ', 'UPDATE', 'DELETE', 'EXPORT', 'IMPORT'
));
```

---

## ISSUE #20: LOW - No Default Behavior for Gender Field

**Location:** `users` table, column `gender` (line 36)  
**Severity:** LOW  
**Category:** Data Quality  

### Current State
```sql
gender VARCHAR(20),  -- ❌ Allows NULL, no default
```

### Problem
- Gender is optional in schema but critical for matching
- Users without gender don't appear in feeds
- NULL gender wastes matching compute
- Should either require or default

### Recommended Fix (Option A: Make Required)
```sql
ALTER TABLE users
ALTER COLUMN gender SET NOT NULL DEFAULT 'OTHER';
```

**OR (Option B: Keep Optional but Add CHECK)**
```sql
ALTER TABLE users
ADD CONSTRAINT valid_gender_value CHECK (
    gender IS NULL 
    OR gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'OTHER')
);
```

---

## ISSUE #21: LOW - Missing Indexes Documented in Schema

**Location:** Table comments/documentation  
**Severity:** LOW  
**Category:** Documentation  

### Problem
- No comments in schema about which indexes exist
- Developers don't know what's already indexed
- Risk of creating redundant indexes

### Recommended Fix
```sql
COMMENT ON TABLE swipes IS 'High-frequency swipe events. Indexes:
  - idx_swipes_user_target (dedup check)
  - idx_swipes_user_time (feed exclusion)
  - idx_swipes_target_user (match detection)
  - idx_swipes_likes (match auto-creation)';
```

---

# SECTION 5: MISSING FEATURES (Not Covered in Audit Scope)

These are not schema errors but worth noting:

1. **No Phone Number Support**
   - Verification codes support PHONE type but users table has no phone column
   - Add: `phone_number VARCHAR(20)` with validation

2. **No Soft Deletes**
   - deleted_at columns exist for messages but not users
   - Consider soft delete pattern for GDPR compliance

3. **No Geographic Indexes**
   - location_lat/location_lng exist but no PostGIS GIST index
   - Would be needed for distance-based queries

4. **No JSON Schema Validation**
   - factors (JSONB), metadata (JSONB), data (JSONB) unconstrained
   - Should document expected structure or add CHECK constraint

5. **No Conversation/Thread Management**
   - Messages reference matches but no conversation grouping
   - Could add conversation_id if needed in future

---

# SECTION 6: SUMMARY & IMPLEMENTATION ROADMAP

## Issue Summary by Category

| Category | Count | Examples |
|----------|-------|----------|
| **Security** | 3 | Message sender validation, password hash, code brute force |
| **Data Integrity** | 6 | Match state, score nullability, verification codes |
| **Logic Consistency** | 4 | Token revocation, notification state, user blocks |
| **Performance** | 2 | Location precision, array size limits |
| **Auditability** | 3 | Message updates, swipe updates, action validation |
| **Best Practices** | 3 | Timezone handling, constraint validation, documentation |

## Implementation Priority

### PHASE 1: CRITICAL (Implement Immediately)
**Estimated Time: 2-3 hours**

```sql
-- 1. Message sender validation
-- 2. Password hash format check
-- 3. Match end state consistency
```

**Files to Modify:**
- `db/init/01-schema.sql` (add constraints/triggers)
- `db/migrations/V3__add_views_and_functions.sql` (add trigger)

### PHASE 2: HIGH (Before Production)
**Estimated Time: 3-4 hours**

```sql
-- 1. Location decimal precision
-- 2. Verification code uniqueness
-- 3. Match scores NOT NULL
-- 4. Message cascade to SET NULL
-- 5. Bio max length
-- 6. Interests array size limit
```

### PHASE 3: MEDIUM (Quality Improvements)
**Estimated Time: 4-5 hours**

```sql
-- All remaining MEDIUM issues
-- Add missing triggers
-- Add state machine validation
```

### PHASE 4: LOW (Polish)
**Estimated Time: 2-3 hours**

```sql
-- Documentation improvements
-- Best practice converterions
-- Optional enhancements
```

## Testing Checklist

After implementing fixes:

- [ ] Attempt to insert message with sender not in match → REJECT
- [ ] Attempt to store non-bcrypt password hash → REJECT
- [ ] Attempt to unmatch without ended_at → REJECT
- [ ] Attempt latitude = 91 → REJECT (precision test)
- [ ] Create two verification codes same type → First invalidated
- [ ] Insert match_score with NULL score → REJECT
- [ ] Delete user with messages → Messages preserved, sender_id = NULL
- [ ] Insert bio > 5000 chars → REJECT or truncate
- [ ] Add 51 interests → REJECT
- [ ] Revoke token without revoked_at → REJECT
- [ ] Insert invalid action to interaction_history → REJECT

---

## Conclusion

The schema demonstrates solid PostgreSQL practices with **14 well-structured core tables** and **50+ optimized indexes**. However, the **3 CRITICAL issues** must be fixed immediately as they represent security and data integrity risks. The **6 HIGH issues** should be addressed before any production deployment.

**Overall Assessment:** Schema is **85/100** - Ready for development with recommended fixes applied. Not production-ready without addressing CRITICAL issues.

**Estimated Total Implementation Time:** 12-16 hours of focused work
