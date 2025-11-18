# PostgreSQL Database Implementation Prompt

## Context
You are responsible for the PostgreSQL database layer of a POC Dating application. The schema exists in `db/init/01-schema.sql` but needs validation, optimization, and production-readiness enhancements. You have **full internet access** to research PostgreSQL optimization, indexing strategies, and performance tuning.

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE CLEAN, MAINTAINABLE, MODULAR SQL.**

This is non-negotiable. Every SQL file must be:
- **MODULAR** - One concern per file, reusable views/functions, clear separation
- **MAINTAINABLE** - Descriptive names, comments on complex logic, consistent formatting
- **CLEAN** - No redundant indexes, no dead queries, proper constraints
- **ABSTRACT** - Use views for complex queries, functions for reusable logic, encapsulate business rules
- **MODERN POSTGRESQL** - Use CTEs, window functions, JSONB, partial indexes, generated columns

**Modularity Rules:**
```sql
-- ✅ GOOD: Separate files by concern
-- 01-schema.sql     (tables only)
-- 02-indexes.sql    (all indexes)
-- 03-views.sql      (materialized views)
-- 04-functions.sql  (stored procedures)
-- 05-seed-data.sql  (test data)

-- ✅ GOOD: Reusable views for complex queries
CREATE VIEW active_matches AS
SELECT * FROM matches WHERE status = 'active';

-- ❌ BAD: 500-line monolithic file mixing everything

-- ✅ GOOD: Modern PostgreSQL features
-- CTEs for readability
WITH active_users AS (
    SELECT * FROM users WHERE last_active > NOW() - INTERVAL '7 days'
)
SELECT * FROM active_users WHERE gender = 'female';

-- Window functions for analytics
SELECT user_id,
       COUNT(*) OVER (PARTITION BY DATE(created_at)) as daily_swipes
FROM swipes;

-- Partial indexes for filtered queries
CREATE INDEX idx_active_matches ON matches(user1_id, user2_id)
WHERE status = 'active';

-- Generated columns for computed values
ALTER TABLE users ADD COLUMN age INT GENERATED ALWAYS AS
    (EXTRACT(YEAR FROM AGE(date_of_birth))) STORED;

-- ❌ BAD: Inline subqueries repeated everywhere
SELECT * FROM users WHERE id IN (SELECT user_id FROM swipes WHERE...);  -- Repeated 10 times
```

**Abstraction Rules:**
```sql
-- ✅ GOOD: Encapsulate complex queries in views
CREATE VIEW feed_candidates AS
SELECT u.*, r.score
FROM users u
LEFT JOIN recommendations r ON r.target_user_id = u.id
WHERE u.is_active = true;

-- ✅ GOOD: Reusable functions for business logic
CREATE FUNCTION calculate_compatibility(user1_id UUID, user2_id UUID)
RETURNS DECIMAL AS $$
    -- Encapsulated scoring logic
$$ LANGUAGE plpgsql;

-- ✅ GOOD: Use function in queries
SELECT *, calculate_compatibility(current_user_id, id) as score
FROM feed_candidates ORDER BY score DESC;

-- ❌ BAD: Copy-paste same 20-line subquery everywhere
```

**Why This Matters:** Multiple agents are working on this codebase. Modular SQL enables independent testing, easier migrations, and faster debugging.

## Scope
1. **Validate** existing schema against API requirements
2. **Optimize** indexes for query patterns
3. **Add** seed data for development/testing
4. **Create** database migrations strategy
5. **Implement** monitoring and maintenance scripts
6. **Document** query patterns and performance expectations

## Critical Resources
- **Current schema:** `db/init/01-schema.sql`
- **API spec:** `docs/API-SPECIFICATION.md` (understand query patterns)
- **Architecture:** `.claude/ARCHITECTURE_PATTERNS.md` (caching integration)
- **DB docs:** `docs/DATABASE-SCHEMA.md`

## Implementation Tasks

### Task 1: Schema Validation
Verify all tables support API operations:
- [ ] `users` - Registration, profiles, preferences
- [ ] `photos` - Photo management with ordering
- [ ] `swipes` - Like/pass with daily limits
- [ ] `matches` - Mutual matches with status
- [ ] `messages` - Chat history with read receipts
- [ ] `recommendations` - ML scores with decay
- [ ] `user_blocks` - Blocking functionality
- [ ] `notifications` - Push notifications
- [ ] `refresh_tokens` - JWT refresh tokens
- [ ] `verification_codes` - Email/phone verification

### Task 2: Index Optimization
Ensure these high-frequency queries are optimized:
```sql
-- User lookup (auth)
SELECT * FROM users WHERE email = ?;
SELECT * FROM users WHERE id = ?;

-- Feed generation (most critical)
SELECT u.* FROM users u
WHERE u.id NOT IN (SELECT target_user_id FROM swipes WHERE user_id = ?)
AND u.gender = ? AND u.age BETWEEN ? AND ?
ORDER BY (SELECT score FROM recommendations WHERE user_id = ? AND target_user_id = u.id) DESC
LIMIT 20;

-- Match queries
SELECT * FROM matches WHERE (user1_id = ? OR user2_id = ?) AND status = 'active';

-- Chat history
SELECT * FROM messages WHERE match_id = ? ORDER BY created_at DESC LIMIT 50;

-- Swipe history (duplicate prevention)
SELECT * FROM swipes WHERE user_id = ? AND target_user_id = ?;
```

Required indexes:
```sql
-- Add if missing
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_gender_age ON users(gender, date_of_birth);
CREATE INDEX IF NOT EXISTS idx_swipes_user_target ON swipes(user_id, target_user_id);
CREATE INDEX IF NOT EXISTS idx_swipes_user_time ON swipes(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_matches_users ON matches(user1_id, user2_id);
CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(status) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_messages_match_time ON messages(match_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_recommendations_user_score ON recommendations(user_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_photos_user_order ON photos(user_id, display_order);
```

### Task 3: Seed Data
Create `db/init/02-seed-data.sql`:
```sql
-- 50+ test users with varied profiles
-- 200+ swipes creating matches
-- 100+ messages in conversations
-- Recommendation scores
-- Diverse demographics for testing filters
```

### Task 4: Performance Views
Create `db/init/03-views.sql`:
```sql
-- Materialized view for feed generation (refresh every 5 min)
CREATE MATERIALIZED VIEW user_feed_candidates AS ...

-- View for match statistics
CREATE VIEW match_stats AS ...

-- View for user activity metrics
CREATE VIEW user_activity AS ...
```

### Task 5: Maintenance Scripts
Create `db/scripts/`:
```sql
-- maintenance.sql: VACUUM, ANALYZE, REINDEX
-- cleanup.sql: Remove old swipes, expired tokens
-- backup.sql: pg_dump commands
-- monitor.sql: Index usage, slow queries, table sizes
```

### Task 6: Migration Strategy
Create `db/migrations/` with Flyway-compatible naming:
```
V1__initial_schema.sql
V2__add_indexes.sql
V3__seed_data.sql
V4__materialized_views.sql
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Schema Validation
```bash
# Reset and apply schema
docker-compose down -v
docker-compose up -d postgres
docker exec -it dating_postgres psql -U dating_user -d dating_db

# In psql:
\dt  -- List tables
\di  -- List indexes
\d+ users  -- Describe table with size
```
- If missing tables/columns → update schema → reset → revalidate

### Phase 2: Index Verification
```sql
-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes ORDER BY idx_scan DESC;

-- Identify missing indexes
SELECT relname, seq_scan, idx_scan, seq_scan - idx_scan AS difference
FROM pg_stat_user_tables WHERE seq_scan > idx_scan ORDER BY difference DESC;
```
- If seq_scan > idx_scan on critical tables → add indexes → re-verify

### Phase 3: Query Performance
```sql
-- Enable timing
\timing on

-- Test critical queries with EXPLAIN ANALYZE
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@test.com';
```
- If any query > 100ms → optimize (add index, rewrite query) → retest
- Use internet to research PostgreSQL query optimization

### Phase 4: Load Testing
```bash
# Insert test data and verify performance holds
# Use pgbench or custom scripts
pgbench -i -s 10 dating_db  # Initialize
pgbench -c 10 -j 2 -t 1000 dating_db  # Run
```
- If performance degrades → analyze → optimize → retest

### Phase 5: Integration Verify
```bash
# Start services and test actual queries
docker-compose up -d
curl -X POST localhost:8080/api/users/auth/register ...
```
- Verify database operations work with actual application

## Success Criteria
- [ ] All 10+ tables exist with correct columns and constraints
- [ ] All critical queries execute in <100ms (EXPLAIN ANALYZE)
- [ ] Indexes cover all WHERE, JOIN, ORDER BY columns
- [ ] Seed data provides realistic test scenarios
- [ ] Materialized views reduce feed generation complexity
- [ ] Maintenance scripts exist and work
- [ ] No sequential scans on large tables

## Performance Targets
| Query Type | Target | Critical |
|------------|--------|----------|
| User lookup by email | <10ms | Auth |
| User lookup by ID | <5ms | Every request |
| Feed generation | <500ms | Core feature |
| Match lookup | <50ms | Match list |
| Chat history | <100ms | Message load |
| Swipe insert | <20ms | User action |

## When Stuck
1. **Search internet** for PostgreSQL indexing, query optimization, performance tuning
2. **Check:** `EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)`
3. **Monitor:** `pg_stat_statements` for slow queries
4. **Read:** PostgreSQL official docs, use-the-index-luke.com

## DO NOT
- Drop tables without backup
- Remove indexes without verifying they're unused
- Use `SELECT *` in production queries
- Skip ANALYZE after bulk inserts
- Ignore foreign key constraints

---
**Iterate until all queries meet performance targets. Use internet access freely to research optimizations.**
