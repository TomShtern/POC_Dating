-- Database Audit Fixes
-- Generated: 2025-11-18
-- This script addresses all CRITICAL and HIGH severity discrepancies

-- =============================================================================
-- PHASE 1: CRITICAL SCHEMA FIXES
-- Apply these immediately to prevent data integrity issues
-- =============================================================================

-- FIX 1.1: Add NOT NULL constraint to users.status
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
COMMENT ON CONSTRAINT valid_status ON users IS 'Enforce valid status values';

-- FIX 1.2: Add ON DELETE SET NULL to matches.ended_by
ALTER TABLE matches 
DROP CONSTRAINT IF EXISTS matches_ended_by_fkey;

ALTER TABLE matches 
ADD CONSTRAINT matches_ended_by_fkey 
    FOREIGN KEY (ended_by) REFERENCES users(id) ON DELETE SET NULL;

-- FIX 1.3: Add ON DELETE SET NULL to reports.resolved_by
ALTER TABLE reports 
DROP CONSTRAINT IF EXISTS reports_resolved_by_fkey;

ALTER TABLE reports 
ADD CONSTRAINT reports_resolved_by_fkey 
    FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL;

-- =============================================================================
-- PHASE 2: INDEX STRATEGY FIXES
-- Apply after Phase 1 to improve query performance
-- =============================================================================

-- FIX 2.1: Add missing ANALYZE statements
-- These ensure query optimizer has correct statistics
ANALYZE match_scores;
ANALYZE refresh_tokens;
ANALYZE user_blocks;
ANALYZE verification_codes;
ANALYZE interaction_history;
ANALYZE reports;
ANALYZE audit_logs;

-- FIX 2.2: Remove redundant index
-- idx_matches_status is redundant with idx_matches_active_user1/user2
-- Save storage and improve write performance
DROP INDEX IF EXISTS idx_matches_status;

-- =============================================================================
-- PHASE 3: DOCUMENTATION FIXES
-- Apply during next maintenance window
-- Add these COMMENT statements to all views and functions
-- =============================================================================

-- FIX 3.1: Add VIEW documentation
COMMENT ON VIEW active_users IS 'Active users with computed age for feed generation';
COMMENT ON VIEW user_profiles IS 'Complete user profile with preferences and stats (optimized with JOIN)';
COMMENT ON VIEW active_matches IS 'Active matches with both user details';
COMMENT ON VIEW conversation_summaries IS 'Match conversations with last message and unread counts';
COMMENT ON VIEW user_stats IS 'User activity statistics for analytics (optimized with CTEs)';
COMMENT ON VIEW match_stats IS 'Daily match statistics for analytics dashboards';
COMMENT ON VIEW swipe_analytics IS 'Daily swipe analytics for monitoring';

-- FIX 3.2: Add Materialized VIEW documentation
COMMENT ON MATERIALIZED VIEW feed_candidates IS 'Pre-computed feed candidates - refresh every 5 minutes';
COMMENT ON MATERIALIZED VIEW daily_swipe_counts IS 'Daily swipe counts for rate limiting - refresh every minute';
COMMENT ON MATERIALIZED VIEW match_activity IS 'Match activity for conversation ordering - refresh every minute';

-- FIX 3.3: Add Function documentation
COMMENT ON FUNCTION refresh_materialized_views() IS 'Refresh all materialized views - call from scheduler';
COMMENT ON FUNCTION calculate_age(DATE) IS 'Calculate age from birth date';
COMMENT ON FUNCTION can_users_match(UUID, UUID) IS 'Check if two users can potentially match based on preferences';
COMMENT ON FUNCTION record_swipe(UUID, UUID, VARCHAR) IS 'Record swipe and automatically create match if mutual like';
COMMENT ON FUNCTION get_user_feed(UUID, INT, INT) IS 'Get feed candidates for user with scoring';
COMMENT ON FUNCTION get_user_matches(UUID, INT, INT) IS 'Get user matches with conversation info (optimized with CTEs)';
COMMENT ON FUNCTION calculate_compatibility(UUID, UUID) IS 'Calculate compatibility score between two users';
COMMENT ON FUNCTION cleanup_old_data(INT, INT, INT) IS 'Clean up expired and old data';
COMMENT ON FUNCTION get_database_stats() IS 'Get table sizes and row counts for monitoring';

-- =============================================================================
-- VERIFICATION QUERIES
-- Run these to verify fixes were applied correctly
-- =============================================================================

-- Verify users.status is NOT NULL
SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'status';

-- Verify foreign key constraints exist and are correct
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name IN ('matches', 'reports') AND constraint_name LIKE '%ended_by%' OR constraint_name LIKE '%resolved_by%';

-- Verify idx_matches_status was removed
SELECT indexname FROM pg_indexes 
WHERE tablename = 'matches' AND indexname = 'idx_matches_status';
-- Should return 0 rows

-- Verify all tables have ANALYZE statistics
SELECT schemaname, tablename, round(n_live_tup::numeric, 0) as row_count
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- Verify all views have comments
SELECT table_name, obj_description(('public.' || table_name)::regclass, 'mv')
FROM information_schema.views
WHERE table_schema = 'public'
ORDER BY table_name;

-- Verify all functions have comments
SELECT p.proname, obj_description(p.oid, 'pg_proc')
FROM pg_proc p
WHERE pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
  AND protype = 'f'
ORDER BY proname;

-- =============================================================================
-- ROLLBACK PROCEDURES (if needed)
-- =============================================================================

/*
-- ROLLBACK FIX 1.1 (NOT NULL on users.status)
ALTER TABLE users ALTER COLUMN status DROP NOT NULL;

-- ROLLBACK FIX 1.2 & 1.3 (restore original FK without ON DELETE SET NULL)
ALTER TABLE matches 
DROP CONSTRAINT IF EXISTS matches_ended_by_fkey;
ALTER TABLE matches 
ADD CONSTRAINT matches_ended_by_fkey 
    FOREIGN KEY (ended_by) REFERENCES users(id);

ALTER TABLE reports 
DROP CONSTRAINT IF EXISTS reports_resolved_by_fkey;
ALTER TABLE reports 
ADD CONSTRAINT reports_resolved_by_fkey 
    FOREIGN KEY (resolved_by) REFERENCES users(id);

-- ROLLBACK FIX 2.2 (recreate idx_matches_status)
CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(status)
    WHERE status = 'ACTIVE';
*/

