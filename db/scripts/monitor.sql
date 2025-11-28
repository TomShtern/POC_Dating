-- POC Dating Database Monitoring Script
-- Version: 2.0
--
-- PURPOSE: Monitor database health and performance
--
-- USAGE:
-- psql -U dating_user -d dating_db -f monitor.sql

\echo '======================================'
\echo 'POC Dating Database Health Report'
\echo 'Generated at:' `date`
\echo '======================================'

-- ========================================
-- 1. DATABASE SIZE
-- ========================================
\echo ''
\echo '>>> Database Size:'

SELECT
    pg_database.datname AS database,
    pg_size_pretty(pg_database_size(pg_database.datname)) AS size
FROM pg_database
WHERE datname = current_database();

-- ========================================
-- 2. TABLE SIZES (Top 10)
-- ========================================
\echo ''
\echo '>>> Table Sizes (Top 10):'

SELECT
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_size_pretty(pg_relation_size(relid)) AS data_size,
    pg_size_pretty(pg_indexes_size(relid)) AS index_size,
    n_live_tup AS rows
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC
LIMIT 10;

-- ========================================
-- 3. INDEX USAGE STATISTICS
-- ========================================
\echo ''
\echo '>>> Index Usage Statistics:'

SELECT
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan AS scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched,
    pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC
LIMIT 20;

-- ========================================
-- 4. SEQUENTIAL SCAN ALERTS
-- Tables with high sequential scans
-- ========================================
\echo ''
\echo '>>> Tables with Sequential Scans > Index Scans (Needs Optimization):'

SELECT
    relname AS table_name,
    seq_scan AS sequential_scans,
    idx_scan AS index_scans,
    seq_scan - idx_scan AS difference,
    CASE
        WHEN idx_scan > 0 THEN ROUND(seq_scan::numeric / idx_scan, 2)
        ELSE seq_scan
    END AS seq_to_idx_ratio
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan
  AND n_live_tup > 1000
ORDER BY difference DESC;

-- ========================================
-- 5. CACHE HIT RATIO
-- Should be > 99%
-- ========================================
\echo ''
\echo '>>> Cache Hit Ratio:'

SELECT
    'Index Hit Ratio' AS metric,
    ROUND(
        (sum(idx_blks_hit)) / nullif(sum(idx_blks_hit + idx_blks_read), 0) * 100,
        2
    ) AS ratio
FROM pg_statio_user_indexes
UNION ALL
SELECT
    'Table Hit Ratio',
    ROUND(
        (sum(heap_blks_hit)) / nullif(sum(heap_blks_hit + heap_blks_read), 0) * 100,
        2
    )
FROM pg_statio_user_tables;

-- ========================================
-- 6. BLOAT DETECTION
-- Tables with dead tuples
-- ========================================
\echo ''
\echo '>>> Table Bloat (Dead Tuples):'

SELECT
    relname AS table_name,
    n_live_tup AS live_tuples,
    n_dead_tup AS dead_tuples,
    CASE
        WHEN n_live_tup > 0
        THEN ROUND(n_dead_tup::numeric / n_live_tup * 100, 2)
        ELSE 0
    END AS dead_pct,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;

-- ========================================
-- 7. LOCK MONITORING
-- Current locks
-- ========================================
\echo ''
\echo '>>> Current Locks:'

SELECT
    pg_class.relname AS table_name,
    pg_locks.mode AS lock_mode,
    pg_locks.granted,
    pg_stat_activity.query AS query,
    pg_stat_activity.state,
    age(now(), pg_stat_activity.query_start) AS duration
FROM pg_locks
JOIN pg_class ON pg_locks.relation = pg_class.oid
JOIN pg_stat_activity ON pg_locks.pid = pg_stat_activity.pid
WHERE pg_class.relname !~ '^pg_'
  AND pg_stat_activity.state != 'idle'
ORDER BY pg_stat_activity.query_start;

-- ========================================
-- 8. ACTIVE CONNECTIONS
-- ========================================
\echo ''
\echo '>>> Active Connections:'

SELECT
    state,
    COUNT(*) AS count,
    MAX(age(now(), query_start)) AS max_duration
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY state;

-- ========================================
-- 9. SLOW QUERIES (if pg_stat_statements enabled)
-- ========================================
\echo ''
\echo '>>> Slow Queries (pg_stat_statements):'

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements') THEN
        RAISE NOTICE 'pg_stat_statements is enabled';
    ELSE
        RAISE NOTICE 'pg_stat_statements not enabled - skipping slow query report';
    END IF;
END $$;

-- ========================================
-- 10. APPLICATION METRICS
-- ========================================
\echo ''
\echo '>>> Application Metrics:'

SELECT 'Total Users' AS metric, COUNT(*)::TEXT AS value FROM users
UNION ALL
SELECT 'Active Users (7d)', COUNT(*)::TEXT FROM users WHERE last_active > NOW() - INTERVAL '7 days'
UNION ALL
SELECT 'Total Matches', COUNT(*)::TEXT FROM matches WHERE status = 'ACTIVE'
UNION ALL
SELECT 'Messages Today', COUNT(*)::TEXT FROM messages WHERE created_at > CURRENT_DATE
UNION ALL
SELECT 'Swipes Today', COUNT(*)::TEXT FROM swipes WHERE created_at > CURRENT_DATE;

-- ========================================
-- 11. REPLICATION STATUS (if applicable)
-- ========================================
\echo ''
\echo '>>> Replication Status:'

SELECT
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    replay_lsn,
    pg_wal_lsn_diff(sent_lsn, replay_lsn) AS lag_bytes
FROM pg_stat_replication;

-- ========================================
-- 12. CHECKPOINT STATISTICS
-- ========================================
\echo ''
\echo '>>> Checkpoint Statistics:'

SELECT
    checkpoints_timed,
    checkpoints_req,
    checkpoint_write_time,
    checkpoint_sync_time,
    buffers_checkpoint,
    buffers_clean,
    buffers_backend
FROM pg_stat_bgwriter;

\echo ''
\echo '======================================'
\echo 'Health report completed!'
\echo '======================================'
