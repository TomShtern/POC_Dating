-- POC Dating Database Maintenance Script
-- Version: 2.0
--
-- PURPOSE: Routine maintenance for optimal performance
--
-- SCHEDULE: Run daily during low-traffic hours (e.g., 3 AM)
--
-- USAGE:
-- psql -U dating_user -d dating_db -f maintenance.sql
-- OR: docker exec -i dating_postgres psql -U dating_user -d dating_db -f /scripts/maintenance.sql

-- ========================================
-- MAINTENANCE CONFIGURATION
-- ========================================
\echo '======================================'
\echo 'POC Dating Database Maintenance'
\echo 'Started at:' `date`
\echo '======================================'

-- Set maintenance-friendly settings
SET maintenance_work_mem = '256MB';
SET work_mem = '64MB';

-- ========================================
-- 1. VACUUM ANALYZE ALL TABLES
-- Reclaim space and update statistics
-- ========================================
\echo ''
\echo '>>> Running VACUUM ANALYZE on all tables...'

VACUUM (VERBOSE, ANALYZE) users;
VACUUM (VERBOSE, ANALYZE) user_preferences;
VACUUM (VERBOSE, ANALYZE) photos;
VACUUM (VERBOSE, ANALYZE) swipes;
VACUUM (VERBOSE, ANALYZE) matches;
VACUUM (VERBOSE, ANALYZE) messages;
VACUUM (VERBOSE, ANALYZE) recommendations;
VACUUM (VERBOSE, ANALYZE) notifications;
VACUUM (VERBOSE, ANALYZE) refresh_tokens;
VACUUM (VERBOSE, ANALYZE) verification_codes;

\echo 'VACUUM ANALYZE completed.'

-- ========================================
-- 2. REINDEX HOT TABLES
-- Rebuild indexes on high-write tables
-- ========================================
\echo ''
\echo '>>> Reindexing high-traffic tables...'

-- Only reindex if bloat > 20%
DO $$
DECLARE
    bloat_ratio NUMERIC;
BEGIN
    -- Check swipes index bloat
    SELECT COALESCE(
        (SELECT (pg_relation_size(indexrelid) - pg_relation_size(indrelid) * 0.1) / pg_relation_size(indexrelid)
         FROM pg_stat_user_indexes WHERE relname = 'swipes' LIMIT 1),
        0
    ) INTO bloat_ratio;

    IF bloat_ratio > 0.2 THEN
        RAISE NOTICE 'Reindexing swipes table (bloat: %)', bloat_ratio;
        REINDEX TABLE swipes;
    END IF;

    -- Check messages index bloat
    SELECT COALESCE(
        (SELECT (pg_relation_size(indexrelid) - pg_relation_size(indrelid) * 0.1) / pg_relation_size(indexrelid)
         FROM pg_stat_user_indexes WHERE relname = 'messages' LIMIT 1),
        0
    ) INTO bloat_ratio;

    IF bloat_ratio > 0.2 THEN
        RAISE NOTICE 'Reindexing messages table (bloat: %)', bloat_ratio;
        REINDEX TABLE messages;
    END IF;
END $$;

\echo 'Reindexing completed.'

-- ========================================
-- 3. REFRESH MATERIALIZED VIEWS
-- Update pre-computed data
-- ========================================
\echo ''
\echo '>>> Refreshing materialized views...'

REFRESH MATERIALIZED VIEW CONCURRENTLY feed_candidates;
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_swipe_counts;
REFRESH MATERIALIZED VIEW CONCURRENTLY match_activity;

\echo 'Materialized views refreshed.'

-- ========================================
-- 4. UPDATE TABLE STATISTICS
-- Ensure query planner has accurate data
-- ========================================
\echo ''
\echo '>>> Updating extended statistics...'

ANALYZE users;
ANALYZE swipes;
ANALYZE matches;
ANALYZE messages;
ANALYZE recommendations;

\echo 'Statistics updated.'

-- ========================================
-- 5. CHECK FOR UNUSED INDEXES
-- Log indexes with 0 scans
-- ========================================
\echo ''
\echo '>>> Checking for unused indexes...'

SELECT
    schemaname || '.' || relname AS table,
    indexrelname AS index,
    pg_size_pretty(pg_relation_size(i.indexrelid)) AS size,
    idx_scan AS scans
FROM pg_stat_user_indexes i
JOIN pg_index USING (indexrelid)
WHERE idx_scan = 0
  AND NOT indisunique
  AND NOT indisprimary
  AND pg_relation_size(i.indexrelid) > 8192
ORDER BY pg_relation_size(i.indexrelid) DESC;

-- ========================================
-- 6. REPORT TABLE SIZES
-- Monitor growth
-- ========================================
\echo ''
\echo '>>> Table sizes report:'

SELECT
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS index_size,
    n_live_tup AS live_rows,
    n_dead_tup AS dead_rows
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;

-- ========================================
-- COMPLETION
-- ========================================
\echo ''
\echo '======================================'
\echo 'Maintenance completed successfully!'
\echo 'Finished at:' `date`
\echo '======================================'
