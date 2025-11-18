-- Flyway Migration V3: Views and Functions
-- POC Dating Application
--
-- This migration adds views and stored procedures

-- ========================================
-- VIEWS
-- ========================================

-- Active users view
CREATE OR REPLACE VIEW active_users AS
SELECT
    id, email, username, first_name, last_name, date_of_birth,
    EXTRACT(YEAR FROM AGE(date_of_birth))::INT AS age,
    gender, bio, profile_picture_url, location_lat, location_lng,
    is_verified, is_premium, last_active, created_at
FROM users
WHERE status = 'ACTIVE' AND date_of_birth IS NOT NULL;

-- Active matches view
CREATE OR REPLACE VIEW active_matches AS
SELECT
    m.id AS match_id, m.user1_id, m.user2_id, m.matched_at,
    u1.username AS user1_username, u1.first_name AS user1_name,
    u2.username AS user2_username, u2.first_name AS user2_name,
    ms.score AS compatibility_score
FROM matches m
JOIN users u1 ON u1.id = m.user1_id
JOIN users u2 ON u2.id = m.user2_id
LEFT JOIN match_scores ms ON ms.match_id = m.id
WHERE m.status = 'ACTIVE';

-- User stats view (optimized with JOINs to avoid N+1)
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

-- Match stats view
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

-- Swipe analytics view
CREATE OR REPLACE VIEW swipe_analytics AS
SELECT
    DATE(created_at) AS swipe_date,
    COUNT(*) AS total_swipes,
    COUNT(*) FILTER (WHERE action = 'LIKE') AS likes,
    COUNT(*) FILTER (WHERE action = 'PASS') AS passes,
    COUNT(*) FILTER (WHERE action = 'SUPER_LIKE') AS super_likes,
    ROUND(COUNT(*) FILTER (WHERE action = 'LIKE')::NUMERIC / NULLIF(COUNT(*), 0) * 100, 2) AS like_rate_pct
FROM swipes
GROUP BY DATE(created_at)
ORDER BY swipe_date DESC;

-- ========================================
-- MATERIALIZED VIEWS
-- ========================================

-- Feed candidates
CREATE MATERIALIZED VIEW IF NOT EXISTS feed_candidates AS
SELECT
    u.id, u.username, u.first_name, u.date_of_birth,
    EXTRACT(YEAR FROM AGE(u.date_of_birth))::INT AS age,
    u.gender, u.bio, u.profile_picture_url, u.location_lat, u.location_lng,
    u.is_verified, u.is_premium, u.last_active, p.interests
FROM users u
LEFT JOIN user_preferences p ON p.user_id = u.id
WHERE u.status = 'ACTIVE' AND u.date_of_birth IS NOT NULL AND u.profile_picture_url IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_feed_candidates_id ON feed_candidates(id);
CREATE INDEX IF NOT EXISTS idx_feed_candidates_gender_age ON feed_candidates(gender, age);
CREATE INDEX IF NOT EXISTS idx_feed_candidates_active ON feed_candidates(last_active DESC);

-- Daily swipe counts
CREATE MATERIALIZED VIEW IF NOT EXISTS daily_swipe_counts AS
SELECT
    user_id, DATE(created_at) AS swipe_date, COUNT(*) AS swipe_count,
    COUNT(*) FILTER (WHERE action = 'LIKE') AS like_count,
    COUNT(*) FILTER (WHERE action = 'SUPER_LIKE') AS super_like_count
FROM swipes
WHERE created_at > CURRENT_DATE - INTERVAL '7 days'
GROUP BY user_id, DATE(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_swipes_user_date ON daily_swipe_counts(user_id, swipe_date);

-- Match activity
CREATE MATERIALIZED VIEW IF NOT EXISTS match_activity AS
SELECT
    m.id AS match_id, m.user1_id, m.user2_id, m.matched_at,
    COALESCE((SELECT MAX(created_at) FROM messages msg WHERE msg.match_id = m.id), m.matched_at) AS last_activity,
    (SELECT COUNT(*) FROM messages msg WHERE msg.match_id = m.id) AS message_count
FROM matches m
WHERE m.status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_match_activity_id ON match_activity(match_id);
CREATE INDEX IF NOT EXISTS idx_match_activity_user1 ON match_activity(user1_id, last_activity DESC);
CREATE INDEX IF NOT EXISTS idx_match_activity_user2 ON match_activity(user2_id, last_activity DESC);

-- ========================================
-- FUNCTIONS
-- ========================================

-- Calculate age function
CREATE OR REPLACE FUNCTION calculate_age(birth_date DATE)
RETURNS INT AS $$
BEGIN
    IF birth_date IS NULL THEN RETURN NULL; END IF;
    RETURN EXTRACT(YEAR FROM AGE(birth_date))::INT;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Record swipe and check match
CREATE OR REPLACE FUNCTION record_swipe(p_user_id UUID, p_target_user_id UUID, p_action VARCHAR(20))
RETURNS TABLE (swipe_id UUID, is_match BOOLEAN, match_id UUID) AS $$
DECLARE
    v_swipe_id UUID;
    v_match_id UUID;
    v_is_match BOOLEAN := FALSE;
    v_user1 UUID;
    v_user2 UUID;
BEGIN
    INSERT INTO swipes (user_id, target_user_id, action)
    VALUES (p_user_id, p_target_user_id, p_action)
    RETURNING id INTO v_swipe_id;

    IF p_action IN ('LIKE', 'SUPER_LIKE') THEN
        IF EXISTS (
            SELECT 1 FROM swipes
            WHERE user_id = p_target_user_id AND target_user_id = p_user_id AND action IN ('LIKE', 'SUPER_LIKE')
        ) THEN
            IF p_user_id < p_target_user_id THEN
                v_user1 := p_user_id; v_user2 := p_target_user_id;
            ELSE
                v_user1 := p_target_user_id; v_user2 := p_user_id;
            END IF;

            INSERT INTO matches (user1_id, user2_id)
            VALUES (v_user1, v_user2)
            ON CONFLICT (user1_id, user2_id) DO NOTHING
            RETURNING id INTO v_match_id;

            IF v_match_id IS NOT NULL THEN
                v_is_match := TRUE;
                INSERT INTO notifications (user_id, type, title, body, data)
                VALUES
                    (p_user_id, 'NEW_MATCH', 'New Match!', 'You have a new match',
                     jsonb_build_object('match_id', v_match_id, 'user_id', p_target_user_id)),
                    (p_target_user_id, 'NEW_MATCH', 'New Match!', 'You have a new match',
                     jsonb_build_object('match_id', v_match_id, 'user_id', p_user_id));
            END IF;
        END IF;
    END IF;

    RETURN QUERY SELECT v_swipe_id, v_is_match, v_match_id;
END;
$$ LANGUAGE plpgsql;

-- Refresh materialized views function
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY feed_candidates;
    REFRESH MATERIALIZED VIEW CONCURRENTLY daily_swipe_counts;
    REFRESH MATERIALIZED VIEW CONCURRENTLY match_activity;
END;
$$ LANGUAGE plpgsql;

-- Cleanup old data function
CREATE OR REPLACE FUNCTION cleanup_old_data(
    p_days_swipes INT DEFAULT 30,
    p_days_recommendations INT DEFAULT 7,
    p_days_verification INT DEFAULT 1
)
RETURNS TABLE (
    swipes_deleted BIGINT,
    recommendations_deleted BIGINT,
    tokens_deleted BIGINT,
    codes_deleted BIGINT
) AS $$
DECLARE
    v_swipes BIGINT;
    v_recommendations BIGINT;
    v_tokens BIGINT;
    v_codes BIGINT;
BEGIN
    WITH deleted AS (DELETE FROM swipes WHERE action = 'PASS' AND created_at < NOW() - (p_days_swipes || ' days')::INTERVAL RETURNING 1)
    SELECT COUNT(*) INTO v_swipes FROM deleted;

    WITH deleted AS (DELETE FROM recommendations WHERE expires_at < NOW() OR created_at < NOW() - (p_days_recommendations || ' days')::INTERVAL RETURNING 1)
    SELECT COUNT(*) INTO v_recommendations FROM deleted;

    WITH deleted AS (DELETE FROM refresh_tokens WHERE expires_at < NOW() OR revoked = true RETURNING 1)
    SELECT COUNT(*) INTO v_tokens FROM deleted;

    WITH deleted AS (DELETE FROM verification_codes WHERE expires_at < NOW() OR used_at IS NOT NULL RETURNING 1)
    SELECT COUNT(*) INTO v_codes FROM deleted;

    RETURN QUERY SELECT v_swipes, v_recommendations, v_tokens, v_codes;
END;
$$ LANGUAGE plpgsql;

-- Get database stats function
CREATE OR REPLACE FUNCTION get_database_stats()
RETURNS TABLE (table_name TEXT, row_count BIGINT, total_size TEXT, index_size TEXT) AS $$
BEGIN
    RETURN QUERY
    SELECT relname::TEXT, reltuples::BIGINT,
        pg_size_pretty(pg_total_relation_size(oid)),
        pg_size_pretty(pg_indexes_size(oid))
    FROM pg_class
    WHERE relkind = 'r' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
    ORDER BY pg_total_relation_size(oid) DESC;
END;
$$ LANGUAGE plpgsql;
