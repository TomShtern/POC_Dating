-- POC Dating Database Cleanup Script
-- Version: 2.0
--
-- PURPOSE: Remove expired and unnecessary data
--
-- SCHEDULE: Run daily after maintenance
--
-- USAGE:
-- psql -U dating_user -d dating_db -f cleanup.sql

\echo '======================================'
\echo 'POC Dating Database Cleanup'
\echo 'Started at:' `date`
\echo '======================================'

-- ========================================
-- CLEANUP CONFIGURATION
-- ========================================
\set swipe_retention_days 30
\set recommendation_retention_days 7
\set notification_retention_days 30
\set audit_retention_days 90

-- ========================================
-- 1. CLEAN UP OLD PASS SWIPES
-- Keep likes for match history, remove passes
-- ========================================
\echo ''
\echo '>>> Cleaning up old PASS swipes...'

WITH deleted AS (
    DELETE FROM swipes
    WHERE action = 'PASS'
      AND created_at < NOW() - INTERVAL '30 days'
    RETURNING 1
)
SELECT COUNT(*) AS pass_swipes_deleted FROM deleted;

-- ========================================
-- 2. CLEAN UP EXPIRED RECOMMENDATIONS
-- ========================================
\echo ''
\echo '>>> Cleaning up expired recommendations...'

WITH deleted AS (
    DELETE FROM recommendations
    WHERE expires_at < NOW()
       OR created_at < NOW() - INTERVAL '7 days'
    RETURNING 1
)
SELECT COUNT(*) AS recommendations_deleted FROM deleted;

-- ========================================
-- 3. CLEAN UP EXPIRED REFRESH TOKENS
-- ========================================
\echo ''
\echo '>>> Cleaning up expired/revoked tokens...'

WITH deleted AS (
    DELETE FROM refresh_tokens
    WHERE expires_at < NOW()
       OR revoked = true
    RETURNING 1
)
SELECT COUNT(*) AS tokens_deleted FROM deleted;

-- ========================================
-- 4. CLEAN UP USED/EXPIRED VERIFICATION CODES
-- ========================================
\echo ''
\echo '>>> Cleaning up verification codes...'

WITH deleted AS (
    DELETE FROM verification_codes
    WHERE expires_at < NOW()
       OR used_at IS NOT NULL
    RETURNING 1
)
SELECT COUNT(*) AS codes_deleted FROM deleted;

-- ========================================
-- 5. CLEAN UP OLD NOTIFICATIONS
-- Keep unread, remove old read notifications
-- ========================================
\echo ''
\echo '>>> Cleaning up old notifications...'

WITH deleted AS (
    DELETE FROM notifications
    WHERE is_read = true
      AND created_at < NOW() - INTERVAL '30 days'
    RETURNING 1
)
SELECT COUNT(*) AS notifications_deleted FROM deleted;

-- ========================================
-- 6. CLEAN UP OLD INTERACTION HISTORY
-- ========================================
\echo ''
\echo '>>> Cleaning up old interaction history...'

WITH deleted AS (
    DELETE FROM interaction_history
    WHERE created_at < NOW() - INTERVAL '90 days'
    RETURNING 1
)
SELECT COUNT(*) AS interactions_deleted FROM deleted;

-- ========================================
-- 7. CLEAN UP OLD AUDIT LOGS
-- ========================================
\echo ''
\echo '>>> Cleaning up old audit logs...'

WITH deleted AS (
    DELETE FROM audit_logs
    WHERE created_at < NOW() - INTERVAL '90 days'
    RETURNING 1
)
SELECT COUNT(*) AS audit_logs_deleted FROM deleted;

-- ========================================
-- 8. CLEAN UP SOFT-DELETED MESSAGES
-- Remove messages marked as deleted over 30 days ago
-- ========================================
\echo ''
\echo '>>> Cleaning up soft-deleted messages...'

WITH deleted AS (
    DELETE FROM messages
    WHERE deleted_at IS NOT NULL
      AND deleted_at < NOW() - INTERVAL '30 days'
    RETURNING 1
)
SELECT COUNT(*) AS messages_deleted FROM deleted;

-- ========================================
-- 9. ARCHIVE INACTIVE USERS (Optional)
-- Log users inactive for 6+ months
-- ========================================
\echo ''
\echo '>>> Inactive users (6+ months):'

SELECT
    COUNT(*) AS inactive_users,
    MIN(last_active) AS oldest_activity
FROM users
WHERE status = 'ACTIVE'
  AND last_active < NOW() - INTERVAL '6 months';

-- ========================================
-- 10. ORPHANED PHOTOS CHECK
-- Photos without valid users
-- ========================================
\echo ''
\echo '>>> Checking for orphaned photos...'

SELECT COUNT(*) AS orphaned_photos
FROM photos p
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = p.user_id);

-- ========================================
-- SUMMARY
-- ========================================
\echo ''
\echo '>>> Space reclaimed summary:'

SELECT
    relname AS table_name,
    n_dead_tup AS dead_tuples_remaining
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;

\echo ''
\echo '======================================'
\echo 'Cleanup completed successfully!'
\echo 'Run VACUUM ANALYZE after cleanup for best results.'
\echo 'Finished at:' `date`
\echo '======================================'
