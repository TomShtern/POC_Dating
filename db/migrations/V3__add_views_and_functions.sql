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

COMMENT ON VIEW active_users IS 'Active users with computed age for feed generation';

-- User profiles view (optimized with JOIN)
CREATE OR REPLACE VIEW user_profiles AS
SELECT
    u.id, u.email, u.username, u.first_name, u.last_name,
    u.date_of_birth,
    EXTRACT(YEAR FROM AGE(u.date_of_birth))::INT AS age,
    u.gender, u.bio, u.profile_picture_url, u.is_verified, u.is_premium,
    u.status, u.last_active, u.created_at,
    p.min_age, p.max_age, p.max_distance_km, p.interested_in, p.interests,
    COALESCE(ph.photo_count, 0) AS photo_count
FROM users u
LEFT JOIN user_preferences p ON p.user_id = u.id
LEFT JOIN (
    SELECT user_id, COUNT(*) as photo_count FROM photos GROUP BY user_id
) ph ON ph.user_id = u.id;

COMMENT ON VIEW user_profiles IS 'Complete user profile with preferences and stats (optimized with JOIN)';

-- Active matches view
CREATE OR REPLACE VIEW active_matches AS
SELECT
    m.id AS match_id, m.user1_id, m.user2_id, m.matched_at,
    u1.username AS user1_username, u1.first_name AS user1_name,
    u1.profile_picture_url AS user1_photo,
    u2.username AS user2_username, u2.first_name AS user2_name,
    u2.profile_picture_url AS user2_photo,
    ms.score AS compatibility_score
FROM matches m
JOIN users u1 ON u1.id = m.user1_id
JOIN users u2 ON u2.id = m.user2_id
LEFT JOIN match_scores ms ON ms.match_id = m.id
WHERE m.status = 'ACTIVE';

COMMENT ON VIEW active_matches IS 'Active matches with both user details';

-- Conversation summaries view
CREATE OR REPLACE VIEW conversation_summaries AS
WITH last_messages AS (
    SELECT DISTINCT ON (match_id)
        match_id, content AS last_message, sender_id AS last_sender_id, created_at AS last_message_time
    FROM messages
    WHERE deleted_at IS NULL
    ORDER BY match_id, created_at DESC
),
unread_counts AS (
    SELECT match_id, sender_id, COUNT(*) AS unread_count
    FROM messages
    WHERE status != 'READ' AND deleted_at IS NULL
    GROUP BY match_id, sender_id
)
SELECT
    m.id AS match_id, m.user1_id, m.user2_id, m.matched_at,
    lm.last_message, lm.last_sender_id, lm.last_message_time,
    COALESCE(uc1.unread_count, 0) AS user1_unread,
    COALESCE(uc2.unread_count, 0) AS user2_unread
FROM matches m
LEFT JOIN last_messages lm ON lm.match_id = m.id
LEFT JOIN unread_counts uc1 ON uc1.match_id = m.id AND uc1.sender_id = m.user2_id
LEFT JOIN unread_counts uc2 ON uc2.match_id = m.id AND uc2.sender_id = m.user1_id
WHERE m.status = 'ACTIVE';

COMMENT ON VIEW conversation_summaries IS 'Match conversations with last message and unread counts';

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

COMMENT ON VIEW user_stats IS 'User activity statistics for analytics (optimized with CTEs)';

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

COMMENT ON VIEW match_stats IS 'Daily match statistics for analytics dashboards';

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

COMMENT ON VIEW swipe_analytics IS 'Daily swipe analytics for monitoring';

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

COMMENT ON MATERIALIZED VIEW feed_candidates IS 'Pre-computed feed candidates - refresh every 5 minutes';

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

COMMENT ON MATERIALIZED VIEW daily_swipe_counts IS 'Daily swipe counts for rate limiting - refresh every minute';

-- Match activity (optimized with CTE)
CREATE MATERIALIZED VIEW IF NOT EXISTS match_activity AS
WITH msg_stats AS (
    SELECT match_id, MAX(created_at) as last_msg_time, COUNT(*) as msg_count
    FROM messages
    WHERE deleted_at IS NULL
    GROUP BY match_id
)
SELECT
    m.id AS match_id, m.user1_id, m.user2_id, m.matched_at,
    COALESCE(ms.last_msg_time, m.matched_at) AS last_activity,
    COALESCE(ms.msg_count, 0) AS message_count
FROM matches m
LEFT JOIN msg_stats ms ON ms.match_id = m.id
WHERE m.status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_match_activity_id ON match_activity(match_id);
CREATE INDEX IF NOT EXISTS idx_match_activity_user1 ON match_activity(user1_id, last_activity DESC);
CREATE INDEX IF NOT EXISTS idx_match_activity_user2 ON match_activity(user2_id, last_activity DESC);

COMMENT ON MATERIALIZED VIEW match_activity IS 'Match activity for conversation ordering - refresh every minute';

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

COMMENT ON FUNCTION calculate_age(DATE) IS 'Calculate age from birth date';

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

COMMENT ON FUNCTION record_swipe(UUID, UUID, VARCHAR) IS 'Record swipe and automatically create match if mutual like';

-- Refresh materialized views function
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY feed_candidates;
    REFRESH MATERIALIZED VIEW CONCURRENTLY daily_swipe_counts;
    REFRESH MATERIALIZED VIEW CONCURRENTLY match_activity;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_materialized_views() IS 'Refresh all materialized views - call from scheduler';

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

COMMENT ON FUNCTION cleanup_old_data(INT, INT, INT) IS 'Clean up expired and old data';

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

COMMENT ON FUNCTION get_database_stats() IS 'Get table sizes and row counts for monitoring';

-- Can users match function
CREATE OR REPLACE FUNCTION can_users_match(user1 UUID, user2 UUID)
RETURNS BOOLEAN AS $$
DECLARE
    u1_age INT; u2_age INT;
    u1_gender VARCHAR(20); u2_gender VARCHAR(20);
    u1_prefs RECORD; u2_prefs RECORD;
BEGIN
    SELECT calculate_age(date_of_birth), gender INTO u1_age, u1_gender
    FROM users WHERE id = user1 AND status = 'ACTIVE';
    SELECT calculate_age(date_of_birth), gender INTO u2_age, u2_gender
    FROM users WHERE id = user2 AND status = 'ACTIVE';
    IF u1_age IS NULL OR u2_age IS NULL THEN RETURN FALSE; END IF;

    SELECT min_age, max_age, interested_in INTO u1_prefs FROM user_preferences WHERE user_id = user1;
    SELECT min_age, max_age, interested_in INTO u2_prefs FROM user_preferences WHERE user_id = user2;

    IF EXISTS (SELECT 1 FROM user_blocks WHERE (blocker_id = user1 AND blocked_id = user2) OR (blocker_id = user2 AND blocked_id = user1)) THEN
        RETURN FALSE;
    END IF;

    IF u2_age < COALESCE(u1_prefs.min_age, 18) OR u2_age > COALESCE(u1_prefs.max_age, 99) THEN RETURN FALSE; END IF;
    IF u1_age < COALESCE(u2_prefs.min_age, 18) OR u1_age > COALESCE(u2_prefs.max_age, 99) THEN RETURN FALSE; END IF;
    IF u1_prefs.interested_in NOT IN ('BOTH', 'EVERYONE') AND u1_prefs.interested_in != u2_gender THEN RETURN FALSE; END IF;
    IF u2_prefs.interested_in NOT IN ('BOTH', 'EVERYONE') AND u2_prefs.interested_in != u1_gender THEN RETURN FALSE; END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION can_users_match(UUID, UUID) IS 'Check if two users can potentially match based on preferences';

-- Get user feed function
CREATE OR REPLACE FUNCTION get_user_feed(p_user_id UUID, p_limit INT DEFAULT 20, p_offset INT DEFAULT 0)
RETURNS TABLE (user_id UUID, username VARCHAR, first_name VARCHAR, age INT, gender VARCHAR, bio TEXT, profile_picture_url VARCHAR, is_verified BOOLEAN, compatibility_score NUMERIC) AS $$
DECLARE
    v_user_prefs RECORD;
BEGIN
    SELECT min_age, max_age, interested_in INTO v_user_prefs FROM user_preferences WHERE user_preferences.user_id = p_user_id;

    RETURN QUERY
    WITH excluded_users AS (
        SELECT target_user_id FROM swipes WHERE swipes.user_id = p_user_id
        UNION SELECT blocked_id FROM user_blocks WHERE blocker_id = p_user_id
        UNION SELECT blocker_id FROM user_blocks WHERE blocked_id = p_user_id
        UNION SELECT CASE WHEN user1_id = p_user_id THEN user2_id ELSE user1_id END FROM matches WHERE user1_id = p_user_id OR user2_id = p_user_id
    )
    SELECT fc.id, fc.username, fc.first_name, fc.age, fc.gender, fc.bio, fc.profile_picture_url, fc.is_verified, COALESCE(r.score, 50.0) AS compatibility_score
    FROM feed_candidates fc
    LEFT JOIN recommendations r ON r.user_id = p_user_id AND r.target_user_id = fc.id
    WHERE fc.id != p_user_id AND fc.id NOT IN (SELECT * FROM excluded_users)
      AND (v_user_prefs IS NULL OR fc.age BETWEEN COALESCE(v_user_prefs.min_age, 18) AND COALESCE(v_user_prefs.max_age, 99))
      AND (v_user_prefs IS NULL OR v_user_prefs.interested_in IS NULL OR v_user_prefs.interested_in IN ('BOTH', 'EVERYONE') OR fc.gender = v_user_prefs.interested_in)
    ORDER BY COALESCE(r.score, 50.0) DESC, fc.last_active DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_user_feed(UUID, INT, INT) IS 'Get feed candidates for user with scoring';

-- Get user matches function (optimized with CTEs)
CREATE OR REPLACE FUNCTION get_user_matches(p_user_id UUID, p_limit INT DEFAULT 20, p_offset INT DEFAULT 0)
RETURNS TABLE (match_id UUID, matched_user_id UUID, matched_username VARCHAR, matched_name VARCHAR, profile_picture_url VARCHAR, matched_at TIMESTAMP, last_message TEXT, last_message_time TIMESTAMP, unread_count BIGINT) AS $$
BEGIN
    RETURN QUERY
    WITH last_messages AS (
        SELECT DISTINCT ON (msg.match_id) msg.match_id, msg.content, msg.created_at
        FROM messages msg WHERE msg.deleted_at IS NULL
        ORDER BY msg.match_id, msg.created_at DESC
    ),
    unread_counts AS (
        SELECT msg.match_id, COUNT(*) as cnt
        FROM messages msg
        WHERE msg.status != 'READ' AND msg.deleted_at IS NULL AND msg.sender_id != p_user_id
        GROUP BY msg.match_id
    )
    SELECT m.id, CASE WHEN m.user1_id = p_user_id THEN m.user2_id ELSE m.user1_id END,
        CASE WHEN m.user1_id = p_user_id THEN u2.username ELSE u1.username END,
        CASE WHEN m.user1_id = p_user_id THEN u2.first_name ELSE u1.first_name END,
        CASE WHEN m.user1_id = p_user_id THEN u2.profile_picture_url ELSE u1.profile_picture_url END,
        m.matched_at, lm.content, lm.created_at, COALESCE(uc.cnt, 0)
    FROM matches m
    JOIN users u1 ON u1.id = m.user1_id
    JOIN users u2 ON u2.id = m.user2_id
    LEFT JOIN last_messages lm ON lm.match_id = m.id
    LEFT JOIN unread_counts uc ON uc.match_id = m.id
    WHERE m.status = 'ACTIVE' AND (m.user1_id = p_user_id OR m.user2_id = p_user_id)
    ORDER BY COALESCE(lm.created_at, m.matched_at) DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_user_matches(UUID, INT, INT) IS 'Get user matches with conversation info (optimized with CTEs)';

-- Calculate compatibility function
CREATE OR REPLACE FUNCTION calculate_compatibility(p_user1_id UUID, p_user2_id UUID)
RETURNS NUMERIC AS $$
DECLARE
    v_score NUMERIC := 50.0;
    v_u1_interests TEXT[]; v_u2_interests TEXT[];
    v_common_interests INT; v_age_diff INT;
BEGIN
    SELECT interests INTO v_u1_interests FROM user_preferences WHERE user_id = p_user1_id;
    SELECT interests INTO v_u2_interests FROM user_preferences WHERE user_id = p_user2_id;

    IF v_u1_interests IS NOT NULL AND v_u2_interests IS NOT NULL THEN
        SELECT COUNT(*) INTO v_common_interests
        FROM unnest(v_u1_interests) i1 JOIN unnest(v_u2_interests) i2 ON LOWER(i1) = LOWER(i2);
        v_score := v_score + LEAST(v_common_interests * 6, 30);
    END IF;

    SELECT ABS(calculate_age((SELECT date_of_birth FROM users WHERE id = p_user1_id)) - calculate_age((SELECT date_of_birth FROM users WHERE id = p_user2_id))) INTO v_age_diff;
    IF v_age_diff <= 5 THEN v_score := v_score + 20;
    ELSIF v_age_diff <= 10 THEN v_score := v_score + 10;
    END IF;

    RETURN LEAST(v_score, 100.0);
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION calculate_compatibility(UUID, UUID) IS 'Calculate compatibility score between two users';

-- Unmatch users function
CREATE OR REPLACE FUNCTION unmatch_users(p_user_id UUID, p_match_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_other_user_id UUID;
    v_user1_id UUID;
    v_user2_id UUID;
BEGIN
    SELECT user1_id, user2_id INTO v_user1_id, v_user2_id
    FROM matches WHERE id = p_match_id AND status = 'ACTIVE';

    IF v_user1_id IS NULL THEN
        RAISE EXCEPTION 'Match % not found or not active', p_match_id;
    END IF;

    IF p_user_id != v_user1_id AND p_user_id != v_user2_id THEN
        RAISE EXCEPTION 'User % is not part of match %', p_user_id, p_match_id;
    END IF;

    v_other_user_id := CASE WHEN p_user_id = v_user1_id THEN v_user2_id ELSE v_user1_id END;

    UPDATE matches SET status = 'UNMATCHED', ended_by = p_user_id, ended_at = NOW(), updated_at = NOW()
    WHERE id = p_match_id;

    INSERT INTO notifications (user_id, type, title, body, data) VALUES
        (p_user_id, 'MATCH_ENDED', 'Match Ended', 'You have unmatched with a user',
         jsonb_build_object('match_id', p_match_id, 'ended_by', p_user_id)),
        (v_other_user_id, 'MATCH_ENDED', 'Match Ended', 'A user has unmatched with you',
         jsonb_build_object('match_id', p_match_id, 'ended_by', p_user_id));

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION unmatch_users(UUID, UUID) IS 'Unmatch users with validation and notifications';

-- Get conversation messages function
CREATE OR REPLACE FUNCTION get_conversation_messages(p_match_id UUID, p_user_id UUID, p_limit INT DEFAULT 50, p_offset INT DEFAULT 0, p_before_id UUID DEFAULT NULL)
RETURNS TABLE (message_id UUID, sender_id UUID, sender_username VARCHAR, sender_name VARCHAR, sender_photo VARCHAR, content TEXT, message_type VARCHAR, status VARCHAR, created_at TIMESTAMP) AS $$
DECLARE
    v_user1_id UUID;
    v_user2_id UUID;
BEGIN
    SELECT user1_id, user2_id INTO v_user1_id, v_user2_id FROM matches WHERE id = p_match_id;

    IF v_user1_id IS NULL THEN
        RAISE EXCEPTION 'Match % not found', p_match_id;
    END IF;

    IF p_user_id != v_user1_id AND p_user_id != v_user2_id THEN
        RAISE EXCEPTION 'User % is not part of match %', p_user_id, p_match_id;
    END IF;

    RETURN QUERY
    SELECT m.id, m.sender_id, u.username, u.first_name, u.profile_picture_url, m.content, m.message_type, m.status, m.created_at
    FROM messages m
    JOIN users u ON u.id = m.sender_id
    WHERE m.match_id = p_match_id AND m.deleted_at IS NULL
      AND (p_before_id IS NULL OR m.created_at < (SELECT created_at FROM messages WHERE id = p_before_id))
    ORDER BY m.created_at DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_conversation_messages(UUID, UUID, INT, INT, UUID) IS 'Get paginated conversation messages';

-- Block user function
CREATE OR REPLACE FUNCTION block_user(p_blocker_id UUID, p_blocked_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_match_id UUID;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = p_blocker_id) THEN
        RAISE EXCEPTION 'Blocker user % not found', p_blocker_id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM users WHERE id = p_blocked_id) THEN
        RAISE EXCEPTION 'Blocked user % not found', p_blocked_id;
    END IF;

    IF p_blocker_id = p_blocked_id THEN
        RAISE EXCEPTION 'Cannot block yourself';
    END IF;

    IF EXISTS (SELECT 1 FROM user_blocks WHERE blocker_id = p_blocker_id AND blocked_id = p_blocked_id) THEN
        RETURN TRUE;
    END IF;

    INSERT INTO user_blocks (blocker_id, blocked_id) VALUES (p_blocker_id, p_blocked_id);

    SELECT id INTO v_match_id FROM matches
    WHERE status = 'ACTIVE' AND ((user1_id = p_blocker_id AND user2_id = p_blocked_id) OR (user1_id = p_blocked_id AND user2_id = p_blocker_id));

    IF v_match_id IS NOT NULL THEN
        UPDATE matches SET status = 'UNMATCHED', ended_by = p_blocker_id, ended_at = NOW(), updated_at = NOW()
        WHERE id = v_match_id;

        INSERT INTO notifications (user_id, type, title, body, data)
        VALUES (p_blocked_id, 'MATCH_ENDED', 'Match Ended', 'A match has ended', jsonb_build_object('match_id', v_match_id));
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION block_user(UUID, UUID) IS 'Block a user and auto-unmatch if matched';

-- Unblock user function
CREATE OR REPLACE FUNCTION unblock_user(p_blocker_id UUID, p_blocked_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_deleted INT;
BEGIN
    DELETE FROM user_blocks WHERE blocker_id = p_blocker_id AND blocked_id = p_blocked_id;
    GET DIAGNOSTICS v_deleted = ROW_COUNT;
    RETURN v_deleted > 0;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION unblock_user(UUID, UUID) IS 'Unblock a user';
