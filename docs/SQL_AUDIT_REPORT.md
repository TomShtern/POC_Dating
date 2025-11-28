# SQL DATABASE AUDIT REPORT
## POC Dating Application - Views & Functions Analysis

**Audit Date:** 2025-11-18
**Files Analyzed:** 
- `/home/user/POC_Dating/db/init/03-views.sql`
- `/home/user/POC_Dating/db/init/04-functions.sql`

**Summary:** 12 issues identified (2 CRITICAL, 4 HIGH, 4 MEDIUM, 2 LOW)

---

## CRITICAL ISSUES

### 1. N+1 Query Pattern in `calculate_compatibility()` Function
**Severity:** CRITICAL  
**File:** 04-functions.sql (Lines 339-341)  
**Impact:** Performance degradation - 2 scalar subqueries execute per compatibility calculation

**Current State:**
```sql
SELECT ABS(
    calculate_age((SELECT date_of_birth FROM users WHERE id = p_user1_id)) -
    calculate_age((SELECT date_of_birth FROM users WHERE id = p_user2_id))
) INTO v_age_diff;
```

**Issue:**
- Two separate scalar subqueries fetch dates from `users` table
- Each call to `calculate_compatibility()` performs 2 additional queries
- Could be 50+ queries if called in a loop (e.g., generating recommendations)

**Recommended Fix:**
```sql
-- Fetch both ages in a single query
DECLARE
    v_user1_age INT;
    v_user2_age INT;
    v_age_diff INT;
BEGIN
    -- ... existing code ...
    
    -- Fetch both dates at once
    SELECT 
        EXTRACT(YEAR FROM AGE(u1.date_of_birth))::INT,
        EXTRACT(YEAR FROM AGE(u2.date_of_birth))::INT
    INTO v_user1_age, v_user2_age
    FROM users u1
    CROSS JOIN users u2
    WHERE u1.id = p_user1_id AND u2.id = p_user2_id;
    
    v_age_diff := ABS(v_user1_age - v_user2_age);
    
    -- ... rest of function ...
END;
```

---

### 2. Missing `unmatch_users()` Function
**Severity:** CRITICAL  
**File:** 04-functions.sql (Not present)  
**Impact:** Unmatching requires manual UPDATE statements; no atomic transaction

**Current State:** No function exists

**Issue:**
- Users cannot unmatch through API without raw SQL
- No transactional guarantee - could orphan match records
- No cleanup of associated data (messages, notifications)
- Status change not audited

**Recommended Fix:**
```sql
CREATE OR REPLACE FUNCTION unmatch_users(
    p_user_id UUID,
    p_match_id UUID
)
RETURNS TABLE (
    success BOOLEAN,
    message TEXT
) AS $$
DECLARE
    v_match_count INT;
    v_is_user_in_match BOOLEAN;
BEGIN
    -- Verify user is part of match
    SELECT COUNT(*) INTO v_match_count
    FROM matches
    WHERE id = p_match_id
      AND (user1_id = p_user_id OR user2_id = p_user_id);
    
    IF v_match_count = 0 THEN
        RETURN QUERY SELECT FALSE, 'User not part of this match';
        RETURN;
    END IF;
    
    -- Update match status
    UPDATE matches
    SET status = 'UNMATCHED', ended_at = NOW(), ended_by = p_user_id
    WHERE id = p_match_id;
    
    -- Notify other user
    WITH match_info AS (
        SELECT CASE WHEN user1_id = p_user_id THEN user2_id ELSE user1_id END AS other_user
        FROM matches WHERE id = p_match_id
    )
    INSERT INTO notifications (user_id, type, title, body, data)
    SELECT other_user, 'MATCH_ENDED', 'Match Ended', 'A match has ended',
           jsonb_build_object('match_id', p_match_id)
    FROM match_info;
    
    RETURN QUERY SELECT TRUE, 'Match ended successfully';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION unmatch_users(UUID, UUID) IS 'Unmatch two users and send notifications';
```

---

## HIGH ISSUES

### 3. Missing `get_conversation_messages()` Function
**Severity:** HIGH  
**File:** 04-functions.sql (Not present)  
**Impact:** Pagination and filtering of messages requires client-side logic

**Current State:** No function exists; messages fetched via direct repository queries

**Issue:**
- No pagination support at database level
- No filtering by date range, message type, or read status
- Every message query loads full match conversation
- Inefficient for large conversations (1000+ messages)

**Recommended Fix:**
```sql
CREATE OR REPLACE FUNCTION get_conversation_messages(
    p_match_id UUID,
    p_limit INT DEFAULT 50,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    sender_id UUID,
    sender_username VARCHAR,
    content TEXT,
    message_type VARCHAR,
    status VARCHAR,
    created_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        m.id,
        m.sender_id,
        u.username,
        m.content,
        m.message_type,
        m.status,
        m.created_at,
        m.delivered_at,
        m.read_at
    FROM messages m
    JOIN users u ON u.id = m.sender_id
    WHERE m.match_id = p_match_id AND m.deleted_at IS NULL
    ORDER BY m.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_conversation_messages(UUID, INT, INT) 
    IS 'Get paginated messages for a conversation';
```

---

### 4. Logic Error in `can_users_match()` - NULL Preference Handling
**Severity:** HIGH  
**File:** 04-functions.sql (Lines 78-86)  
**Impact:** Users without preferences cannot match; preference check fails on NULL

**Current State:**
```sql
-- Lines 78-86
IF u1_prefs.interested_in NOT IN ('BOTH', 'EVERYONE')
   AND u1_prefs.interested_in != u2_gender THEN
    RETURN FALSE;
END IF;

IF u2_prefs.interested_in NOT IN ('BOTH', 'EVERYONE')
   AND u2_prefs.interested_in != u1_gender THEN
    RETURN FALSE;
END IF;
```

**Issue:**
- If `u1_prefs.interested_in` is NULL (new user, no preferences set), the comparison fails
- NULL IN (...) evaluates to UNKNOWN, not TRUE/FALSE
- Prevents new users from appearing in feeds

**Recommended Fix:**
```sql
-- Use COALESCE to handle NULL preferences (default to BOTH/accepting all)
IF COALESCE(u1_prefs.interested_in, 'BOTH') NOT IN ('BOTH', 'EVERYONE')
   AND COALESCE(u1_prefs.interested_in, 'BOTH') != u2_gender THEN
    RETURN FALSE;
END IF;

IF COALESCE(u2_prefs.interested_in, 'BOTH') NOT IN ('BOTH', 'EVERYONE')
   AND COALESCE(u2_prefs.interested_in, 'BOTH') != u1_gender THEN
    RETURN FALSE;
END IF;
```

---

### 5. Missing Error Handling in `record_swipe()` Function
**Severity:** HIGH  
**File:** 04-functions.sql (Lines 98-159)  
**Impact:** Silent failures; no feedback if swipe fails (duplicate, blocked user, etc.)

**Current State:**
```sql
CREATE OR REPLACE FUNCTION record_swipe(
    p_user_id UUID,
    p_target_user_id UUID,
    p_action VARCHAR(20)
)
RETURNS TABLE (...)
AS $$
BEGIN
    -- No input validation
    INSERT INTO swipes (user_id, target_user_id, action)
    VALUES (p_user_id, p_target_user_id, p_action)
    RETURNING id INTO v_swipe_id;
    -- If duplicate or self-swipe, INSERT fails silently
END;
$$ LANGUAGE plpgsql;
```

**Issue:**
- No validation for self-swipes (should be prevented by constraint, but handled ungracefully)
- No check for blocked users before recording swipe
- No validation of action enum values
- If constraint violation occurs, entire transaction rolls back
- Client has no way to know why swipe failed

**Recommended Fix:**
```sql
CREATE OR REPLACE FUNCTION record_swipe(
    p_user_id UUID,
    p_target_user_id UUID,
    p_action VARCHAR(20)
)
RETURNS TABLE (
    swipe_id UUID,
    is_match BOOLEAN,
    match_id UUID,
    error_message TEXT
) AS $$
DECLARE
    v_swipe_id UUID;
    v_match_id UUID;
    v_is_match BOOLEAN := FALSE;
    v_error TEXT := NULL;
BEGIN
    -- Input validation
    IF p_user_id = p_target_user_id THEN
        RETURN QUERY SELECT NULL::UUID, FALSE, NULL::UUID, 'Cannot swipe on yourself'::TEXT;
        RETURN;
    END IF;
    
    IF p_action NOT IN ('LIKE', 'PASS', 'SUPER_LIKE') THEN
        RETURN QUERY SELECT NULL::UUID, FALSE, NULL::UUID, 'Invalid action'::TEXT;
        RETURN;
    END IF;
    
    -- Check if users are blocked
    IF EXISTS (
        SELECT 1 FROM user_blocks
        WHERE (blocker_id = p_user_id AND blocked_id = p_target_user_id)
           OR (blocker_id = p_target_user_id AND blocked_id = p_user_id)
    ) THEN
        RETURN QUERY SELECT NULL::UUID, FALSE, NULL::UUID, 'User is blocked'::TEXT;
        RETURN;
    END IF;
    
    -- Check for duplicate swipe
    IF EXISTS (SELECT 1 FROM swipes WHERE user_id = p_user_id AND target_user_id = p_target_user_id) THEN
        RETURN QUERY SELECT NULL::UUID, FALSE, NULL::UUID, 'Already swiped on this user'::TEXT;
        RETURN;
    END IF;
    
    -- Record swipe with error handling
    BEGIN
        INSERT INTO swipes (user_id, target_user_id, action)
        VALUES (p_user_id, p_target_user_id, p_action)
        RETURNING id INTO v_swipe_id;
    EXCEPTION WHEN OTHERS THEN
        RETURN QUERY SELECT NULL::UUID, FALSE, NULL::UUID, SQLERRM::TEXT;
        RETURN;
    END;
    
    -- Check for match...
    -- [rest of function]
    
    RETURN QUERY SELECT v_swipe_id, v_is_match, v_match_id, v_error;
END;
$$ LANGUAGE plpgsql;
```

---

## MEDIUM ISSUES

### 6. Missing `get_database_stats()` Function Marker
**Severity:** MEDIUM  
**File:** 04-functions.sql (Line 423)  
**Impact:** Query planner cannot optimize; function may be executed multiple times

**Current State:**
```sql
CREATE OR REPLACE FUNCTION get_database_stats()
RETURNS TABLE (...)
AS $$
BEGIN
    RETURN QUERY
    SELECT relname::TEXT, reltuples::BIGINT, ...
    FROM pg_class
    WHERE relkind = 'r' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
    ORDER BY pg_total_relation_size(oid) DESC;
END;
$$ LANGUAGE plpgsql;
```

**Issue:**
- Function should be marked `STABLE` or `IMMUTABLE` (it only reads catalog tables)
- Optimizer may call function multiple times unnecessarily
- Query plans cannot be cached effectively

**Recommended Fix:**
```sql
CREATE OR REPLACE FUNCTION get_database_stats()
RETURNS TABLE (...)
AS $$
BEGIN
    RETURN QUERY ...
END;
$$ LANGUAGE plpgsql STABLE;  -- Add STABLE marker

COMMENT ON FUNCTION get_database_stats() IS 'Get table sizes and row counts for monitoring (cached)';
```

---

### 7. `user_stats` View Could Benefit from Materialization
**Severity:** MEDIUM  
**File:** 03-views.sql (Lines 146-191)  
**Impact:** Dashboard queries slow; multiple CTEs with GROUP BY on large tables

**Current State:**
```sql
CREATE OR REPLACE VIEW user_stats AS
WITH swipe_stats AS (
    SELECT user_id, COUNT(*) AS total_swipes, ...
    FROM swipes
    GROUP BY user_id
),
match_stats AS (
    SELECT user_id, COUNT(*) AS total_matches, ...
    FROM matches ...
    GROUP BY user_id
),
message_stats AS (
    SELECT sender_id AS user_id, COUNT(*) AS messages_sent
    FROM messages
    GROUP BY sender_id
)
SELECT u.id, u.username, COALESCE(ss.total_swipes, 0), ...
FROM users u
LEFT JOIN swipe_stats ss ON ss.user_id = u.id
LEFT JOIN match_stats ms ON ms.user_id = u.id
LEFT JOIN message_stats msg ON msg.user_id = u.id;
```

**Issue:**
- Three CTEs perform expensive GROUP BY operations on high-volume tables
- Used frequently for dashboards and analytics
- Each query scans full swipes, matches, and messages tables
- Could be pre-computed hourly instead

**Recommended Fix:**
```sql
-- Convert to materialized view with hourly refresh
CREATE MATERIALIZED VIEW IF NOT EXISTS user_stats AS
WITH swipe_stats AS (
    SELECT user_id, COUNT(*) AS total_swipes, ...
    FROM swipes
    GROUP BY user_id
),
-- ... rest of query ...
SELECT u.id, u.username, COALESCE(ss.total_swipes, 0), ...
FROM users u
LEFT JOIN swipe_stats ss ON ss.user_id = u.id
LEFT JOIN match_stats ms ON ms.user_id = u.id
LEFT JOIN message_stats msg ON msg.user_id = u.id;

-- Add unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_stats_id ON user_stats(user_id);

-- Add refresh to scheduler: REFRESH MATERIALIZED VIEW CONCURRENTLY user_stats;
-- In refresh_materialized_views() function, add:
--     REFRESH MATERIALIZED VIEW CONCURRENTLY user_stats;
```

---

### 8. `match_stats` and `swipe_analytics` Views Should Be Materialized
**Severity:** MEDIUM  
**File:** 03-views.sql (Lines 197-234)  
**Impact:** Slow dashboard queries; GROUP BY on large tables for analytics

**Current State:**
```sql
CREATE OR REPLACE VIEW match_stats AS
SELECT
    DATE(m.matched_at) AS match_date,
    COUNT(*) AS total_matches,
    COUNT(*) FILTER (WHERE m.status = 'ACTIVE') AS active_matches,
    -- ... other aggregations
FROM matches m
LEFT JOIN match_scores ms ON ms.match_id = m.id
LEFT JOIN messages msg ON msg.match_id = m.id
GROUP BY DATE(m.matched_at)
ORDER BY match_date DESC;
```

**Issue:**
- Complex aggregation with multiple JOINs scanned on every query
- Dashboard queries hit this view multiple times per load
- Pre-computed data doesn't change frequently enough to justify live computation

**Recommended Fix:**
```sql
-- Convert both views to materialized
CREATE MATERIALIZED VIEW IF NOT EXISTS match_stats AS
-- ... existing query ...

CREATE UNIQUE INDEX IF NOT EXISTS idx_match_stats_date ON match_stats(match_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS swipe_analytics AS
-- ... existing query ...

CREATE UNIQUE INDEX IF NOT EXISTS idx_swipe_analytics_date ON swipe_analytics(swipe_date);

-- Update refresh_materialized_views() function:
-- REFRESH MATERIALIZED VIEW CONCURRENTLY match_stats;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY swipe_analytics;
```

---

### 9. Inefficient Subquery in `user_profiles` View
**Severity:** MEDIUM  
**File:** 03-views.sql (Lines 68-70)  
**Impact:** Subquery repeated for every user; missing index optimization

**Current State:**
```sql
CREATE OR REPLACE VIEW user_profiles AS
SELECT
    -- ... user columns ...
    COALESCE(ph.photo_count, 0) AS photo_count
FROM users u
LEFT JOIN user_preferences p ON p.user_id = u.id
LEFT JOIN (
    SELECT user_id, COUNT(*) as photo_count FROM photos GROUP BY user_id
) ph ON ph.user_id = u.id;
```

**Issue:**
- Inline subquery is recomputed for every view query
- No way to cache/index subquery results
- Could benefit from pre-aggregation

**Recommended Fix:**
```sql
-- Create materialized view for photo counts (updated hourly)
CREATE MATERIALIZED VIEW IF NOT EXISTS user_photo_stats AS
SELECT user_id, COUNT(*) as photo_count
FROM photos
GROUP BY user_id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_photo_stats_user_id ON user_photo_stats(user_id);

-- Update user_profiles view to use materialized view
CREATE OR REPLACE VIEW user_profiles AS
SELECT
    u.id,
    -- ... other columns ...
    COALESCE(ph.photo_count, 0) AS photo_count
FROM users u
LEFT JOIN user_preferences p ON p.user_id = u.id
LEFT JOIN user_photo_stats ph ON ph.user_id = u.id;
```

---

## LOW ISSUES

### 10. `refresh_materialized_views()` Function Lacks Error Handling
**Severity:** LOW  
**File:** 03-views.sql (Lines 323-332)  
**Impact:** Silent failures during maintenance; scheduler unaware of issues

**Current State:**
```sql
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY feed_candidates;
    REFRESH MATERIALIZED VIEW CONCURRENTLY daily_swipe_counts;
    REFRESH MATERIALIZED VIEW CONCURRENTLY match_activity;
END;
$$ LANGUAGE plpgsql;
```

**Issue:**
- No error logging if REFRESH fails
- No transaction control (partial success possible)
- Scheduler has no feedback mechanism

**Recommended Fix:**
```sql
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS TABLE (
    view_name TEXT,
    status TEXT,
    error_message TEXT
) AS $$
DECLARE
    v_error TEXT;
BEGIN
    -- Refresh feed_candidates
    BEGIN
        REFRESH MATERIALIZED VIEW CONCURRENTLY feed_candidates;
        RETURN QUERY SELECT 'feed_candidates'::TEXT, 'success'::TEXT, NULL::TEXT;
    EXCEPTION WHEN OTHERS THEN
        v_error := SQLERRM;
        RETURN QUERY SELECT 'feed_candidates'::TEXT, 'error'::TEXT, v_error::TEXT;
    END;
    
    -- Refresh daily_swipe_counts
    BEGIN
        REFRESH MATERIALIZED VIEW CONCURRENTLY daily_swipe_counts;
        RETURN QUERY SELECT 'daily_swipe_counts'::TEXT, 'success'::TEXT, NULL::TEXT;
    EXCEPTION WHEN OTHERS THEN
        v_error := SQLERRM;
        RETURN QUERY SELECT 'daily_swipe_counts'::TEXT, 'error'::TEXT, v_error::TEXT;
    END;
    
    -- Refresh match_activity
    BEGIN
        REFRESH MATERIALIZED VIEW CONCURRENTLY match_activity;
        RETURN QUERY SELECT 'match_activity'::TEXT, 'success'::TEXT, NULL::TEXT;
    EXCEPTION WHEN OTHERS THEN
        v_error := SQLERRM;
        RETURN QUERY SELECT 'match_activity'::TEXT, 'error'::TEXT, v_error::TEXT;
    END;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_materialized_views() 
    IS 'Refresh all materialized views with error reporting';
```

---

### 11. Missing `block_user()` and `unblock_user()` Functions
**Severity:** LOW  
**File:** 04-functions.sql (Not present)  
**Impact:** Blocking requires raw SQL; no transactional safety

**Current State:** No functions exist; user_blocks table accessed directly

**Recommended Fix:**
```sql
CREATE OR REPLACE FUNCTION block_user(
    p_blocker_id UUID,
    p_blocked_id UUID,
    p_reason VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    success BOOLEAN,
    message TEXT
) AS $$
BEGIN
    IF p_blocker_id = p_blocked_id THEN
        RETURN QUERY SELECT FALSE, 'Cannot block yourself'::TEXT;
        RETURN;
    END IF;
    
    INSERT INTO user_blocks (blocker_id, blocked_id, reason)
    VALUES (p_blocker_id, p_blocked_id, p_reason)
    ON CONFLICT (blocker_id, blocked_id) DO NOTHING;
    
    RETURN QUERY SELECT TRUE, 'User blocked successfully'::TEXT;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION block_user(UUID, UUID, VARCHAR) 
    IS 'Block a user from viewing your profile';

CREATE OR REPLACE FUNCTION unblock_user(
    p_blocker_id UUID,
    p_blocked_id UUID
)
RETURNS TABLE (
    success BOOLEAN,
    message TEXT
) AS $$
BEGIN
    DELETE FROM user_blocks 
    WHERE blocker_id = p_blocker_id AND blocked_id = p_blocked_id;
    
    RETURN QUERY SELECT TRUE, 'User unblocked successfully'::TEXT;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION unblock_user(UUID, UUID) 
    IS 'Unblock a previously blocked user';
```

---

### 12. Minor: Comment Enhancement for `refresh_materialized_views()` Function
**Severity:** LOW  
**File:** 03-views.sql (Line 332)  
**Impact:** Developer confusion; schedule refresh interval not documented

**Current State:**
```sql
COMMENT ON FUNCTION refresh_materialized_views() IS 'Refresh all materialized views - call from scheduler';
```

**Issue:**
- No recommendation on refresh frequency
- Unclear which views are refreshed
- No guidance on manual vs scheduled execution

**Recommended Fix:**
```sql
COMMENT ON FUNCTION refresh_materialized_views() IS 
    'Refresh all materialized views for caching. Schedule via cron:
     - feed_candidates: every 5 minutes (feed generation performance)
     - daily_swipe_counts: every 1 minute (rate limiting accuracy)
     - match_activity: every 1 minute (conversation ordering)
     - user_stats (optional): every 1 hour (analytics dashboards)
     - match_stats (optional): every 5 minutes (admin dashboards)
     - swipe_analytics (optional): every 5 minutes (analytics)
     Example: SELECT cron.schedule(''refresh_mvs'', ''*/1 * * * *'', 
              ''SELECT refresh_materialized_views()'');';
```

---

## SUMMARY TABLE

| Issue # | Type | Severity | Component | Impact |
|---------|------|----------|-----------|--------|
| 1 | N+1 Queries | CRITICAL | `calculate_compatibility()` | 50+ queries in loops |
| 2 | Missing Function | CRITICAL | `unmatch_users()` | No atomic unmatch |
| 3 | Missing Function | HIGH | `get_conversation_messages()` | No pagination |
| 4 | Logic Error | HIGH | `can_users_match()` | NULL handling broken |
| 5 | Error Handling | HIGH | `record_swipe()` | Silent failures |
| 6 | Missing Marker | MEDIUM | `get_database_stats()` | Query optimizer issue |
| 7 | Materialization | MEDIUM | `user_stats` | Dashboard slowness |
| 8 | Materialization | MEDIUM | `match_stats`, `swipe_analytics` | Analytics slowness |
| 9 | Subquery Inefficiency | MEDIUM | `user_profiles` | Photo count repeated |
| 10 | Error Handling | LOW | `refresh_materialized_views()` | Silent failures |
| 11 | Missing Functions | LOW | `block_user()`, `unblock_user()` | No atomic blocking |
| 12 | Documentation | LOW | `refresh_materialized_views()` | Schedule unclear |

---

## RECOMMENDATIONS PRIORITY

**Immediate (Do First):**
1. Fix N+1 query in `calculate_compatibility()` - CRITICAL performance issue
2. Implement `unmatch_users()` - required for app functionality
3. Add error handling to `record_swipe()` - affects user experience

**Short-term (This Sprint):**
4. Implement `get_conversation_messages()` - required for messaging
5. Fix NULL handling in `can_users_match()` - blocks new user onboarding
6. Materialize `user_stats`, `match_stats`, `swipe_analytics` - dashboard performance

**Medium-term (Next Sprint):**
7. Add `block_user()` and `unblock_user()` functions
8. Improve error handling in `refresh_materialized_views()`
9. Optimize `user_profiles` subquery
10. Add STABLE marker to `get_database_stats()`

---

## TESTING RECOMMENDATIONS

After implementing fixes:

```sql
-- Test unmatch_users()
SELECT unmatch_users('user1-uuid', 'match-uuid');

-- Test get_conversation_messages() with pagination
SELECT * FROM get_conversation_messages('match-uuid', 50, 0);

-- Test record_swipe() error handling
SELECT * FROM record_swipe('user-id', 'user-id', 'LIKE'); -- Should error

-- Test can_users_match() with NULL preferences
INSERT INTO users VALUES (...); -- No preference record
SELECT can_users_match('new-user-uuid', 'existing-user-uuid'); -- Should work

-- Test calculate_compatibility() performance
EXPLAIN ANALYZE SELECT calculate_compatibility('user1', 'user2');
-- Should show single query plan, not multiple subqueries

-- Verify materialized view refresh
SELECT refresh_materialized_views();
SELECT COUNT(*) FROM feed_candidates; -- Should be current
```

