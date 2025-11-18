-- POC Dating Database Views
-- Version: 2.0
--
-- PURPOSE: Reusable views and materialized views for complex queries
--
-- BENEFITS:
-- - Encapsulate complex business logic
-- - Reduce code duplication across services
-- - Enable query optimization through materialized views

-- ========================================
-- ACTIVE USERS VIEW
-- Users available for matching
-- ========================================
CREATE OR REPLACE VIEW active_users AS
SELECT
    id,
    email,
    username,
    first_name,
    last_name,
    date_of_birth,
    EXTRACT(YEAR FROM AGE(date_of_birth))::INT AS age,
    gender,
    bio,
    profile_picture_url,
    location_lat,
    location_lng,
    is_verified,
    is_premium,
    last_active,
    created_at
FROM users
WHERE status = 'ACTIVE'
  AND date_of_birth IS NOT NULL;

COMMENT ON VIEW active_users IS 'Active users with computed age for feed generation';

-- ========================================
-- USER PROFILE VIEW
-- Complete user profile with preferences
-- ========================================
CREATE OR REPLACE VIEW user_profiles AS
SELECT
    u.id,
    u.email,
    u.username,
    u.first_name,
    u.last_name,
    u.date_of_birth,
    EXTRACT(YEAR FROM AGE(u.date_of_birth))::INT AS age,
    u.gender,
    u.bio,
    u.profile_picture_url,
    u.is_verified,
    u.is_premium,
    u.status,
    u.last_active,
    u.created_at,
    p.min_age,
    p.max_age,
    p.max_distance_km,
    p.interested_in,
    p.interests,
    (SELECT COUNT(*) FROM photos ph WHERE ph.user_id = u.id) AS photo_count
FROM users u
LEFT JOIN user_preferences p ON p.user_id = u.id;

COMMENT ON VIEW user_profiles IS 'Complete user profile with preferences and stats';

-- ========================================
-- ACTIVE MATCHES VIEW
-- Active matches with user details
-- ========================================
CREATE OR REPLACE VIEW active_matches AS
SELECT
    m.id AS match_id,
    m.user1_id,
    m.user2_id,
    m.matched_at,
    u1.username AS user1_username,
    u1.first_name AS user1_name,
    u1.profile_picture_url AS user1_photo,
    u2.username AS user2_username,
    u2.first_name AS user2_name,
    u2.profile_picture_url AS user2_photo,
    ms.score AS compatibility_score
FROM matches m
JOIN users u1 ON u1.id = m.user1_id
JOIN users u2 ON u2.id = m.user2_id
LEFT JOIN match_scores ms ON ms.match_id = m.id
WHERE m.status = 'ACTIVE';

COMMENT ON VIEW active_matches IS 'Active matches with both user details';

-- ========================================
-- CONVERSATION SUMMARY VIEW
-- Match conversations with last message
-- ========================================
CREATE OR REPLACE VIEW conversation_summaries AS
WITH last_messages AS (
    SELECT DISTINCT ON (match_id)
        match_id,
        content AS last_message,
        sender_id AS last_sender_id,
        created_at AS last_message_time
    FROM messages
    WHERE deleted_at IS NULL
    ORDER BY match_id, created_at DESC
),
unread_counts AS (
    SELECT
        match_id,
        sender_id,
        COUNT(*) AS unread_count
    FROM messages
    WHERE status != 'READ'
      AND deleted_at IS NULL
    GROUP BY match_id, sender_id
)
SELECT
    m.id AS match_id,
    m.user1_id,
    m.user2_id,
    m.matched_at,
    lm.last_message,
    lm.last_sender_id,
    lm.last_message_time,
    COALESCE(uc1.unread_count, 0) AS user1_unread,
    COALESCE(uc2.unread_count, 0) AS user2_unread
FROM matches m
LEFT JOIN last_messages lm ON lm.match_id = m.id
LEFT JOIN unread_counts uc1 ON uc1.match_id = m.id AND uc1.sender_id = m.user2_id
LEFT JOIN unread_counts uc2 ON uc2.match_id = m.id AND uc2.sender_id = m.user1_id
WHERE m.status = 'ACTIVE';

COMMENT ON VIEW conversation_summaries IS 'Match conversations with last message and unread counts';

-- ========================================
-- USER STATS VIEW
-- User activity statistics (optimized with JOINs to avoid N+1)
-- ========================================
CREATE OR REPLACE VIEW user_stats AS
WITH swipe_stats AS (
    SELECT
        user_id,
        COUNT(*) AS total_swipes,
        COUNT(*) FILTER (WHERE action = 'LIKE') AS total_likes,
        COUNT(*) FILTER (WHERE action = 'SUPER_LIKE') AS super_likes
    FROM swipes
    GROUP BY user_id
),
match_stats AS (
    SELECT
        user_id,
        COUNT(*) AS total_matches,
        COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active_matches
    FROM (
        SELECT user1_id AS user_id, status FROM matches
        UNION ALL
        SELECT user2_id AS user_id, status FROM matches
    ) m
    GROUP BY user_id
),
message_stats AS (
    SELECT
        sender_id AS user_id,
        COUNT(*) AS messages_sent
    FROM messages
    GROUP BY sender_id
)
SELECT
    u.id AS user_id,
    u.username,
    u.created_at AS registered_at,
    u.last_active,
    COALESCE(ss.total_swipes, 0) AS total_swipes,
    COALESCE(ss.total_likes, 0) AS total_likes,
    COALESCE(ss.super_likes, 0) AS super_likes,
    COALESCE(ms.total_matches, 0) AS total_matches,
    COALESCE(ms.active_matches, 0) AS active_matches,
    COALESCE(msg.messages_sent, 0) AS messages_sent
FROM users u
LEFT JOIN swipe_stats ss ON ss.user_id = u.id
LEFT JOIN match_stats ms ON ms.user_id = u.id
LEFT JOIN message_stats msg ON msg.user_id = u.id;

COMMENT ON VIEW user_stats IS 'User activity statistics for analytics (optimized with CTEs)';

-- ========================================
-- MATCH STATS VIEW
-- Aggregate match statistics for analytics
-- ========================================
CREATE OR REPLACE VIEW match_stats AS
SELECT
    DATE(m.matched_at) AS match_date,
    COUNT(*) AS total_matches,
    COUNT(*) FILTER (WHERE m.status = 'ACTIVE') AS active_matches,
    COUNT(*) FILTER (WHERE m.status = 'UNMATCHED') AS unmatched,
    AVG(ms.score) AS avg_compatibility_score,
    COUNT(DISTINCT msg.match_id) AS matches_with_messages,
    SUM(CASE WHEN msg.id IS NOT NULL THEN 1 ELSE 0 END) AS total_messages
FROM matches m
LEFT JOIN match_scores ms ON ms.match_id = m.id
LEFT JOIN messages msg ON msg.match_id = m.id
GROUP BY DATE(m.matched_at)
ORDER BY match_date DESC;

COMMENT ON VIEW match_stats IS 'Daily match statistics for analytics dashboards';

-- ========================================
-- SWIPE ANALYTICS VIEW
-- Swipe patterns and conversion rates
-- ========================================
CREATE OR REPLACE VIEW swipe_analytics AS
SELECT
    DATE(created_at) AS swipe_date,
    COUNT(*) AS total_swipes,
    COUNT(*) FILTER (WHERE action = 'LIKE') AS likes,
    COUNT(*) FILTER (WHERE action = 'PASS') AS passes,
    COUNT(*) FILTER (WHERE action = 'SUPER_LIKE') AS super_likes,
    ROUND(
        COUNT(*) FILTER (WHERE action = 'LIKE')::NUMERIC / NULLIF(COUNT(*), 0) * 100,
        2
    ) AS like_rate_pct
FROM swipes
GROUP BY DATE(created_at)
ORDER BY swipe_date DESC;

COMMENT ON VIEW swipe_analytics IS 'Daily swipe analytics for monitoring';

-- ========================================
-- MATERIALIZED VIEW: Feed Candidates
-- Pre-computed for feed generation performance
-- ========================================
CREATE MATERIALIZED VIEW IF NOT EXISTS feed_candidates AS
SELECT
    u.id,
    u.username,
    u.first_name,
    u.date_of_birth,
    EXTRACT(YEAR FROM AGE(u.date_of_birth))::INT AS age,
    u.gender,
    u.bio,
    u.profile_picture_url,
    u.location_lat,
    u.location_lng,
    u.is_verified,
    u.is_premium,
    u.last_active,
    p.interests
FROM users u
LEFT JOIN user_preferences p ON p.user_id = u.id
WHERE u.status = 'ACTIVE'
  AND u.date_of_birth IS NOT NULL
  AND u.profile_picture_url IS NOT NULL;

-- Unique index required for CONCURRENT refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_feed_candidates_id ON feed_candidates(id);

-- Performance indexes on materialized view
CREATE INDEX IF NOT EXISTS idx_feed_candidates_gender_age ON feed_candidates(gender, age);
CREATE INDEX IF NOT EXISTS idx_feed_candidates_active ON feed_candidates(last_active DESC);

COMMENT ON MATERIALIZED VIEW feed_candidates IS 'Pre-computed feed candidates - refresh every 5 minutes';

-- ========================================
-- MATERIALIZED VIEW: Daily Swipe Counts
-- For rate limiting
-- ========================================
CREATE MATERIALIZED VIEW IF NOT EXISTS daily_swipe_counts AS
SELECT
    user_id,
    DATE(created_at) AS swipe_date,
    COUNT(*) AS swipe_count,
    COUNT(*) FILTER (WHERE action = 'LIKE') AS like_count,
    COUNT(*) FILTER (WHERE action = 'SUPER_LIKE') AS super_like_count
FROM swipes
WHERE created_at > CURRENT_DATE - INTERVAL '7 days'
GROUP BY user_id, DATE(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_swipes_user_date ON daily_swipe_counts(user_id, swipe_date);

COMMENT ON MATERIALIZED VIEW daily_swipe_counts IS 'Daily swipe counts for rate limiting - refresh every minute';

-- ========================================
-- MATERIALIZED VIEW: Match Activity
-- For conversation list ordering
-- ========================================
CREATE MATERIALIZED VIEW IF NOT EXISTS match_activity AS
SELECT
    m.id AS match_id,
    m.user1_id,
    m.user2_id,
    m.matched_at,
    COALESCE(
        (SELECT MAX(created_at) FROM messages msg WHERE msg.match_id = m.id),
        m.matched_at
    ) AS last_activity,
    (SELECT COUNT(*) FROM messages msg WHERE msg.match_id = m.id) AS message_count
FROM matches m
WHERE m.status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_match_activity_id ON match_activity(match_id);
CREATE INDEX IF NOT EXISTS idx_match_activity_user1 ON match_activity(user1_id, last_activity DESC);
CREATE INDEX IF NOT EXISTS idx_match_activity_user2 ON match_activity(user2_id, last_activity DESC);

COMMENT ON MATERIALIZED VIEW match_activity IS 'Match activity for conversation ordering - refresh every minute';

-- ========================================
-- REFRESH FUNCTION for Materialized Views
-- ========================================
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY feed_candidates;
    REFRESH MATERIALIZED VIEW CONCURRENTLY daily_swipe_counts;
    REFRESH MATERIALIZED VIEW CONCURRENTLY match_activity;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_materialized_views() IS 'Refresh all materialized views - call from scheduler';
