# POC Dating Database

PostgreSQL database layer for the dating application.

## Directory Structure

```
db/
├── init/                    # Initialization scripts (run in order)
│   ├── 01-schema.sql       # Core tables and constraints
│   ├── 02-indexes.sql      # Performance indexes
│   ├── 03-views.sql        # Views and materialized views
│   ├── 04-functions.sql    # Stored procedures
│   └── 05-seed-data.sql    # Test data
├── scripts/                 # Maintenance scripts
│   ├── maintenance.sql     # VACUUM, ANALYZE, REINDEX
│   ├── cleanup.sql         # Remove expired data
│   ├── monitor.sql         # Health monitoring
│   └── backup.sh           # Backup with retention
└── migrations/              # Flyway migrations
    ├── V1__initial_schema.sql
    ├── V2__add_indexes.sql
    └── V3__add_views_and_functions.sql
```

## Quick Start

### Using Docker Compose
```bash
# Start PostgreSQL with schema initialization
docker-compose up -d postgres

# Connect to database
docker exec -it dating_postgres psql -U dating_user -d dating_db
```

### Manual Setup
```bash
# Run init scripts in order
psql -U dating_user -d dating_db -f db/init/01-schema.sql
psql -U dating_user -d dating_db -f db/init/02-indexes.sql
psql -U dating_user -d dating_db -f db/init/03-views.sql
psql -U dating_user -d dating_db -f db/init/04-functions.sql
psql -U dating_user -d dating_db -f db/init/05-seed-data.sql
```

## Tables

| Table | Purpose | Key Indexes |
|-------|---------|-------------|
| users | User profiles & auth | email, gender+dob |
| user_preferences | Matching preferences | user_id, interests (GIN) |
| photos | User photos | user_id+order |
| swipes | Like/pass events | user+target, user+time |
| matches | Mutual matches | user1+user2, status |
| messages | Chat messages | match+time |
| recommendations | ML scores | user+score |
| notifications | Push notifications | user_id, is_read |
| refresh_tokens | JWT management | user_id, expires_at |
| verification_codes | Email/phone verify | user_id+type |
| user_blocks | Block functionality | blocker+blocked |
| reports | User reports | status |

## Performance Indexes

Optimized for these critical queries:

### User Lookup (Auth)
```sql
-- idx_users_email
SELECT * FROM users WHERE email = ?;
```

### Feed Generation
```sql
-- idx_users_gender_dob + idx_swipes_user_target
SELECT * FROM feed_candidates
WHERE gender = ? AND age BETWEEN ? AND ?
  AND id NOT IN (SELECT target_user_id FROM swipes WHERE user_id = ?)
```

### Match List
```sql
-- idx_matches_active_user1/user2
SELECT * FROM matches WHERE (user1_id = ? OR user2_id = ?) AND status = 'ACTIVE'
```

### Chat History
```sql
-- idx_messages_match_time
SELECT * FROM messages WHERE match_id = ? ORDER BY created_at DESC
```

## Materialized Views

Refresh these for optimal performance:

```sql
-- Refresh all views (call from scheduler)
SELECT refresh_materialized_views();

-- Manual refresh
REFRESH MATERIALIZED VIEW CONCURRENTLY feed_candidates;
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_swipe_counts;
REFRESH MATERIALIZED VIEW CONCURRENTLY match_activity;
```

**Refresh Schedule:**
- `feed_candidates`: Every 5 minutes
- `daily_swipe_counts`: Every 1 minute
- `match_activity`: Every 1 minute

## Maintenance

### Daily Maintenance
```bash
# Run during low-traffic hours (e.g., 3 AM)
psql -U dating_user -d dating_db -f db/scripts/maintenance.sql
psql -U dating_user -d dating_db -f db/scripts/cleanup.sql
```

### Monitoring
```bash
# Health check
psql -U dating_user -d dating_db -f db/scripts/monitor.sql
```

### Backups
```bash
# Full backup with retention
./db/scripts/backup.sh full

# Schema only
./db/scripts/backup.sh schema

# List backups
./db/scripts/backup.sh list
```

## Performance Targets

| Query Type | Target | Achieved |
|------------|--------|----------|
| User lookup by email | <10ms | Use idx_users_email |
| User lookup by ID | <5ms | Primary key |
| Feed generation | <500ms | Materialized view |
| Match lookup | <50ms | Partial indexes |
| Chat history | <100ms | Composite index |
| Swipe insert | <20ms | Minimal indexes |

## Stored Procedures

### record_swipe
Atomic swipe + match creation:
```sql
SELECT * FROM record_swipe(user_id, target_user_id, 'LIKE');
-- Returns: swipe_id, is_match, match_id
```

### get_user_feed
Optimized feed generation:
```sql
SELECT * FROM get_user_feed(user_id, 20, 0);
```

### get_user_matches
Matches with conversation info:
```sql
SELECT * FROM get_user_matches(user_id, 20, 0);
```

### cleanup_old_data
Remove expired records:
```sql
SELECT * FROM cleanup_old_data(30, 7, 1);
-- Returns count of deleted records
```

## Test Data

The `05-seed-data.sql` provides:
- 50 diverse test users (25 male, 25 female)
- User preferences for each user
- Photos for each user
- 200+ swipes with match scenarios
- 5 active matches with conversations
- 15+ messages in conversations
- Recommendation scores
- Sample notifications

**Test Credentials:**
- All users: Password `Password123!`
- Example: `emma@test.com` / `Password123!`

## Flyway Migrations

For production, use Flyway with these migrations:

```bash
# Run migrations
flyway -url=jdbc:postgresql://localhost:5432/dating_db \
       -user=dating_user -password=xxx migrate
```

Migration order:
1. `V1__initial_schema.sql` - Tables
2. `V2__add_indexes.sql` - Indexes
3. `V3__add_views_and_functions.sql` - Views + Functions

## Troubleshooting

### Slow Queries
```sql
-- Check for sequential scans
SELECT relname, seq_scan, idx_scan
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan;

-- Check index usage
SELECT * FROM pg_stat_user_indexes ORDER BY idx_scan DESC;
```

### High Dead Tuples
```sql
-- Check bloat
SELECT relname, n_dead_tup, last_autovacuum
FROM pg_stat_user_tables ORDER BY n_dead_tup DESC;

-- Force vacuum
VACUUM (VERBOSE, ANALYZE) table_name;
```

### Missing Index
```sql
-- Find queries not using indexes
EXPLAIN ANALYZE SELECT ...;

-- Add index
CREATE INDEX CONCURRENTLY idx_name ON table(column);
```

## Security Notes

- Passwords are BCrypt hashed (12 rounds)
- No PII in logs
- SQL injection prevented via parameterized queries
- Refresh tokens have device tracking
- Audit logs track all changes
