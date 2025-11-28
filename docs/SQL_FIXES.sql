-- ============================================================================
-- SQL AUDIT FIXES - POC Dating Application
-- Date: 2025-11-18
-- 
-- CRITICAL FIXES (Apply Immediately)
-- ============================================================================

-- ============================================================================
-- FIX #1: Calculate Compatibility - Replace N+1 Query
-- ============================================================================
-- FILE: 04-functions.sql (Lines 339-341)
-- REPLACE THIS:
/*
    SELECT ABS(
        calculate_age((SELECT date_of_birth FROM users WHERE id = p_user1_id)) -
        calculate_age((SELECT date_of_birth FROM users WHERE id = p_user2_id))
    ) INTO v_age_diff;
*/

-- WITH THIS:
CREATE OR REPLACE FUNCTION calculate_compatibility(
    p_user1_id UUID,
    p_user2_id UUID
)
RETURNS NUMERIC AS $$
DECLARE
    v_score NUMERIC := 50.0;
    v_u1_interests TEXT[];
    v_u2_interests TEXT[];
    v_common_interests INT;
    v_age_diff INT;
    v_user1_age INT;
    v_user2_age INT;
BEGIN
    -- Get interests
    SELECT interests INTO v_u1_interests
    FROM user_preferences WHERE user_id = p_user1_id;

    SELECT interests INTO v_u2_interests
    FROM user_preferences WHERE user_id = p_user2_id;

    -- Calculate common interests (max 30 points)
    IF v_u1_interests IS NOT NULL AND v_u2_interests IS NOT NULL THEN
        SELECT COUNT(*) INTO v_common_interests
        FROM unnest(v_u1_interests) i1
        JOIN unnest(v_u2_interests) i2 ON LOWER(i1) = LOWER(i2);

        v_score := v_score + LEAST(v_common_interests * 6, 30);
    END IF;

    -- FIX: Fetch BOTH ages in single query instead of two separate subqueries
    SELECT 
        EXTRACT(YEAR FROM AGE(u1.date_of_birth))::INT,
        EXTRACT(YEAR FROM AGE(u2.date_of_birth))::INT
    INTO v_user1_age, v_user2_age
    FROM users u1
    CROSS JOIN users u2
    WHERE u1.id = p_user1_id AND u2.id = p_user2_id;
    
    -- Age compatibility (max 20 points)
    IF v_user1_age IS NOT NULL AND v_user2_age IS NOT NULL THEN
        v_age_diff := ABS(v_user1_age - v_user2_age);
        
        IF v_age_diff <= 5 THEN
            v_score := v_score + 20;
        ELSIF v_age_diff <= 10 THEN
            v_score := v_score + 10;
        END IF;
    END IF;

    RETURN LEAST(v_score, 100.0);
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION calculate_compatibility(UUID, UUID) 
    IS 'Calculate compatibility score between two users (optimized - single query for ages)';


-- ============================================================================
-- FIX #2: Add unmatch_users() Function (CRITICAL - MISSING)
-- ============================================================================
-- Add to: 04-functions.sql
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
BEGIN
    -- Verify user is part of match
    SELECT COUNT(*) INTO v_match_count
    FROM matches
    WHERE id = p_match_id
      AND (user1_id = p_user_id OR user2_id = p_user_id);
    
    IF v_match_count = 0 THEN
        RETURN QUERY SELECT FALSE, 'User not part of this match'::TEXT;
        RETURN;
    END IF;
    
    -- Update match status atomically
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
    
    RETURN QUERY SELECT TRUE, 'Match ended successfully'::TEXT;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION unmatch_users(UUID, UUID) 
    IS 'Unmatch two users and send notifications (atomic transaction)';


-- ============================================================================
-- HIGH PRIORITY FIXES
-- ============================================================================

-- ============================================================================
-- FIX #3: Add get_conversation_messages() Function (HIGH - MISSING)
-- ============================================================================
-- Add to: 04-functions.sql
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
    IS 'Get paginated messages for a conversation with sender info';


-- ============================================================================
-- FIX #4: Fix can_users_match() NULL Preference Handling (HIGH)
-- ============================================================================
-- FILE: 04-functions.sql (Lines 78-86)
-- REPLACE THESE LINES:
/*
    IF u1_prefs.interested_in NOT IN ('BOTH', 'EVERYONE')
       AND u1_prefs.interested_in != u2_gender THEN
        RETURN FALSE;
    END IF;

    IF u2_prefs.interested_in NOT IN ('BOTH', 'EVERYONE')
       AND u2_prefs.interested_in != u1_gender THEN
        RETURN FALSE;
    END IF;
*/

-- WITH THIS:
CREATE OR REPLACE FUNCTION can_users_match(user1 UUID, user2 UUID)
RETURNS BOOLEAN AS $$
DECLARE
    u1_age INT;
    u2_age INT;
    u1_gender VARCHAR(20);
    u2_gender VARCHAR(20);
    u1_prefs RECORD;
    u2_prefs RECORD;
BEGIN
    -- Get user details
    SELECT calculate_age(date_of_birth), gender INTO u1_age, u1_gender
    FROM users WHERE id = user1 AND status = 'ACTIVE';

    SELECT calculate_age(date_of_birth), gender INTO u2_age, u2_gender
    FROM users WHERE id = user2 AND status = 'ACTIVE';

    -- Check if users exist and are active
    IF u1_age IS NULL OR u2_age IS NULL THEN
        RETURN FALSE;
    END IF;

    -- Get preferences
    SELECT min_age, max_age, interested_in INTO u1_prefs
    FROM user_preferences WHERE user_id = user1;

    SELECT min_age, max_age, interested_in INTO u2_prefs
    FROM user_preferences WHERE user_id = user2;

    -- Check blocks
    IF EXISTS (
        SELECT 1 FROM user_blocks
        WHERE (blocker_id = user1 AND blocked_id = user2)
           OR (blocker_id = user2 AND blocked_id = user1)
    ) THEN
        RETURN FALSE;
    END IF;

    -- Check age preferences (both ways)
    IF u2_age < COALESCE(u1_prefs.min_age, 18) OR u2_age > COALESCE(u1_prefs.max_age, 99) THEN
        RETURN FALSE;
    END IF;

    IF u1_age < COALESCE(u2_prefs.min_age, 18) OR u1_age > COALESCE(u2_prefs.max_age, 99) THEN
        RETURN FALSE;
    END IF;

    -- FIX: Handle NULL preferences properly with COALESCE
    -- Gender preferences (both ways) - if no preference, accept all
    IF COALESCE(u1_prefs.interested_in, 'BOTH') NOT IN ('BOTH', 'EVERYONE')
       AND COALESCE(u1_prefs.interested_in, 'BOTH') != u2_gender THEN
        RETURN FALSE;
    END IF;

    IF COALESCE(u2_prefs.interested_in, 'BOTH') NOT IN ('BOTH', 'EVERYONE')
       AND COALESCE(u2_prefs.interested_in, 'BOTH') != u1_gender THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION can_users_match(UUID, UUID) 
    IS 'Check if two users can potentially match based on preferences (fixed NULL handling)';


-- ============================================================================
-- FIX #5: Add error_message to record_swipe() return type (HIGH)
-- ============================================================================
-- FILE: 04-functions.sql (Lines 98-159)
-- MODIFY RETURN TYPE TABLE to add error_message:
/*
RETURNS TABLE (
    swipe_id UUID,
    is_match BOOLEAN,
    match_id UUID,
    error_message TEXT    -- ADD THIS COLUMN
) AS $$
*/

-- Add validation before INSERT:
/*
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
    
    -- Rest of function...
END;
*/


-- ============================================================================
-- MEDIUM PRIORITY FIXES
-- ============================================================================

-- ============================================================================
-- FIX #6: Add STABLE marker to get_database_stats()
-- ============================================================================
-- FILE: 04-functions.sql (Line 442)
-- CHANGE:
/*
$$ LANGUAGE plpgsql;
*/

-- TO:
/*
$$ LANGUAGE plpgsql STABLE;
*/

-- Update comment:
/*
COMMENT ON FUNCTION get_database_stats() 
    IS 'Get table sizes and row counts for monitoring (cached results)';
*/


-- ============================================================================
-- FIX #7: Convert user_stats to materialized view
-- ============================================================================
-- FILE: 03-views.sql (Lines 146-191)
-- REPLACE "CREATE OR REPLACE VIEW" WITH "CREATE MATERIALIZED VIEW IF NOT EXISTS"
-- Add indexes:

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_stats_user_id ON user_stats(user_id);

-- Add to refresh_materialized_views() function:
-- REFRESH MATERIALIZED VIEW CONCURRENTLY user_stats;


-- ============================================================================
-- FIX #8: Convert analytics views to materialized views
-- ============================================================================
-- FILE: 03-views.sql
-- REPLACE "CREATE OR REPLACE VIEW" WITH "CREATE MATERIALIZED VIEW IF NOT EXISTS"

-- For match_stats (lines 197-210):
CREATE UNIQUE INDEX IF NOT EXISTS idx_match_stats_match_date ON match_stats(match_date DESC);

-- For swipe_analytics (lines 218-231):
CREATE UNIQUE INDEX IF NOT EXISTS idx_swipe_analytics_swipe_date ON swipe_analytics(swipe_date DESC);

-- Add to refresh_materialized_views() function:
-- REFRESH MATERIALIZED VIEW CONCURRENTLY match_stats;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY swipe_analytics;


-- ============================================================================
-- LOW PRIORITY FIXES
-- ============================================================================

-- ============================================================================
-- FIX #9: Add block_user() and unblock_user() functions
-- ============================================================================
-- Add to: 04-functions.sql

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


-- ============================================================================
-- FIX #10: Improve refresh_materialized_views() with error handling
-- ============================================================================
-- FILE: 03-views.sql (Lines 323-332)
-- REPLACE ENTIRE FUNCTION:

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

COMMENT ON FUNCTION refresh_materialized_views() IS 
    'Refresh all materialized views with error reporting. Schedule refresh:
     - feed_candidates: every 5 minutes (feed generation)
     - daily_swipe_counts: every 1 minute (rate limiting)
     - match_activity: every 1 minute (conversation ordering)
     Usage: SELECT refresh_materialized_views();';

