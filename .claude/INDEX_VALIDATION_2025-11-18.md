# Index Validation Report - POC Dating Database

## Status: INDEXES PRESENT BUT INEFFECTUAL AGAINST N+1 PATTERNS

---

## Index Coverage Analysis

### 1. PHOTOS_USER_ORDER Index (Line 62)
**Index**: `CREATE INDEX idx_photos_user_order ON photos(user_id, display_order);`

**Used by**: user_profiles view (line 65 scalar subquery)

**Status**: ✓ Index exists

**Problem**: Even with index, the scalar subquery approach means:
- Single user profile request = 1 query
- 50 user profiles = 50 separate queries using this index
- **Total queries: Still 50** (index just makes each one faster)

**Verdict**: Index helps individual query speed but doesn't solve N+1 architecture issue.

---

### 2. SWIPES Indexes (Lines 78-90)
**Indexes**:
- `idx_swipes_user_target ON swipes(user_id, target_user_id)` (line 78)
- `idx_swipes_user_time ON swipes(user_id, created_at DESC)` (line 82)
- `idx_swipes_likes ON swipes(target_user_id, user_id, action)` (line 89)

**Used by**: user_stats view (lines 149-151 scalar subqueries)

**Status**: ✓ Index exists

**Problem**: Same as above
- 100 users = 100 separate COUNT(*) queries to swipes table
- Each index makes one count fast
- **Total queries: Still 100-300** (6 different aggregations × 100 rows)

**Verdict**: Indexes help but don't solve architectural problem.

---

### 3. MESSAGES Indexes (Lines 127-138)
**Indexes**:
- `idx_messages_match_time ON messages(match_id, created_at DESC)` (line 127)
- `idx_messages_unread ON messages(match_id, status)` (line 133)
- `idx_messages_recent ON messages(match_id, created_at DESC)` (line 137)

**Used by**: get_user_matches() function (lines 266-286)

**Status**: ✓ Index exists

**Problem**: Index solves point lookup, but function uses 4 separate scalar subqueries
- Per match: 4 separate queries to messages table
- 20 matches = 80 queries (even with perfect indexes)

**Verdict**: Indexes help but don't solve the scalar subquery problem.

---

## Root Cause Analysis: Why Indexes Aren't Enough

### The Scalar Subquery Anti-Pattern
```sql
-- This view definition:
SELECT
    u.id,
    (SELECT COUNT(*) FROM photos WHERE user_id = u.id) AS photo_count
FROM users u

-- Is fundamentally equivalent to:
for each user u:
    SELECT COUNT(*) FROM photos WHERE user_id = u.id
```

**Even with a perfect index**, you're still executing N separate queries.

### PostgreSQL Cannot Optimize This
PostgreSQL's query planner sees:
```
Loop through users
  ├─ For each row, run subquery: (SELECT COUNT(*) FROM photos...)
  ├─ Index lookup: ✓ Fast
  └─ But this happens 100 times (for 100 users)
```

The only optimization available is to make each individual lookup faster (indexes do this).
The architectural issue remains: **N queries instead of 1**.

---

## Recommended Query Pattern Comparison

### Current Pattern (Scalar Subquery)
```sql
SELECT
    u.id,
    (SELECT COUNT(*) FROM photos WHERE user_id = u.id) AS photo_count
FROM users u
LIMIT 100;

-- Execution Plan:
-- Seq Scan on users (100 rows)
--   -> Index Scan on idx_photos_user_order (100 times)
--
-- Total Queries: 101 (1 users scan + 100 photo counts)
-- Est. Time: 500-1000ms
```

### Optimized Pattern (LEFT JOIN + GROUP BY)
```sql
SELECT
    u.id,
    COUNT(ph.id) AS photo_count
FROM users u
LEFT JOIN photos ph ON ph.user_id = u.id
GROUP BY u.id
LIMIT 100;

-- Execution Plan:
-- Hash Aggregate (100 groups)
--   -> Hash Join (users LEFT JOIN photos)
--     -> Seq Scan on users (100 rows)
--     -> Index Scan on idx_photos_user_order (1 sequential)
--
-- Total Queries: 1 (single plan)
-- Est. Time: 50-100ms (10x faster)
```

---

## Index Validation: Missing Indexes?

Checking against view/function query patterns:

### ✓ Index Present: `idx_swipes_likes`
```sql
CREATE INDEX idx_swipes_likes ON swipes(target_user_id, user_id, action)
WHERE action IN ('LIKE', 'SUPER_LIKE');
```
- Used by: record_swipe() function, get_user_feed() function
- Status: ✓ Present (line 89)

### ✓ Index Present: `idx_messages_match_time`
```sql
CREATE INDEX idx_messages_match_time ON messages(match_id, created_at DESC);
```
- Used by: get_user_matches() for last message queries
- Status: ✓ Present (line 127)

### ✓ Index Present: `idx_messages_unread`
```sql
CREATE INDEX idx_messages_unread ON messages(match_id, status)
WHERE status != 'READ' AND deleted_at IS NULL;
```
- Used by: conversation_summaries view, get_user_matches()
- Status: ✓ Present (line 133)

### ✓ Index Present: `idx_matches_active_user1` and `idx_matches_active_user2`
```sql
CREATE INDEX idx_matches_active_user1 ON matches(user1_id, matched_at DESC)
WHERE status = 'ACTIVE';
CREATE INDEX idx_matches_active_user2 ON matches(user2_id, matched_at DESC)
WHERE status = 'ACTIVE';
```
- Used by: get_user_matches(), conversation_summaries views
- Status: ✓ Present (lines 102-105)

### ✓ Index Present: `idx_swipes_target_user`
```sql
CREATE INDEX idx_swipes_target_user ON swipes(target_user_id);
```
- Used by: get_user_feed() exclusion logic
- Status: ✓ Present (line 86)

---

## Conclusion: Indexes Are NOT The Problem

**The indexes are comprehensive and well-designed.** The performance issues stem from:

1. **View/Function Architecture** (PRIMARY ISSUE)
   - Use of scalar subqueries instead of JOINs
   - Causes N+1 query patterns regardless of index quality
   
2. **Materialized View Refresh Efficiency** (SECONDARY ISSUE)
   - Subqueries in materialized views cause slow refresh cycles
   - Should be converted to JOINs with aggregation

3. **Missing Error Handling** (SECONDARY ISSUE)
   - Functions don't gracefully handle individual component failures
   - All-or-nothing approach to materialized view refresh

---

## Validation Summary

| Index | Purpose | Status | Notes |
|-------|---------|--------|-------|
| idx_photos_user_order | Photos lookup | ✓ Present | But doesn't fix N+1 in user_profiles |
| idx_swipes_user_target | Swipe lookup | ✓ Present | Good for duplicate checks |
| idx_swipes_user_time | Daily counts | ✓ Present | Helps but N+1 still exists |
| idx_swipes_likes | Match detection | ✓ Present | Good for record_swipe |
| idx_messages_match_time | Chat history | ✓ Present | But N+1 exists in get_user_matches |
| idx_messages_unread | Unread count | ✓ Present | Good but N+1 still exists |
| idx_matches_active_user1 | Match queries | ✓ Present | Properly configured |
| idx_matches_active_user2 | Match queries | ✓ Present | Properly configured |
| idx_user_preferences_user_id | Preference lookup | ✓ Present | Used by can_users_match |
| idx_users_gender_age | Feed filtering | ✓ Present | Good for feed generation |

**Result**: All necessary indexes are present and well-designed. Performance improvements must come from fixing the view/function SQL patterns, not from indexing.

---

**Generated**: 2025-11-18
