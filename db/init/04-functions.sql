-- POC Dating Database Functions
-- Version: 2.0
--
-- PURPOSE: Reusable stored procedures for business logic
--
-- BENEFITS:
-- - Encapsulate complex operations
-- - Ensure atomic transactions
-- - Reduce network round-trips

-- ========================================
-- FUNCTION: Calculate User Age
-- ========================================
CREATE OR REPLACE FUNCTION calculate_age(birth_date DATE)
RETURNS INT AS $$
BEGIN
    IF birth_date IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN EXTRACT(YEAR FROM AGE(birth_date))::INT;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION calculate_age(DATE) IS 'Calculate age from birth date';

-- ========================================
-- FUNCTION: Check if Users Can Match
-- Validates mutual preferences and blocks
-- ========================================
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

    -- Check gender preferences (both ways)
    IF u1_prefs.interested_in NOT IN ('BOTH', 'EVERYONE')
       AND u1_prefs.interested_in != u2_gender THEN
        RETURN FALSE;
    END IF;

    IF u2_prefs.interested_in NOT IN ('BOTH', 'EVERYONE')
       AND u2_prefs.interested_in != u1_gender THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION can_users_match(UUID, UUID) IS 'Check if two users can potentially match based on preferences';

-- ========================================
-- FUNCTION: Record Swipe and Check Match
-- Atomic swipe + match creation
-- ========================================
CREATE OR REPLACE FUNCTION record_swipe(
    p_user_id UUID,
    p_target_user_id UUID,
    p_action VARCHAR(20)
)
RETURNS TABLE (
    swipe_id UUID,
    is_match BOOLEAN,
    match_id UUID
) AS $$
DECLARE
    v_swipe_id UUID;
    v_match_id UUID;
    v_is_match BOOLEAN := FALSE;
    v_user1 UUID;
    v_user2 UUID;
BEGIN
    -- Insert swipe
    INSERT INTO swipes (user_id, target_user_id, action)
    VALUES (p_user_id, p_target_user_id, p_action)
    RETURNING id INTO v_swipe_id;

    -- Check for match only if LIKE or SUPER_LIKE
    IF p_action IN ('LIKE', 'SUPER_LIKE') THEN
        -- Check if target already liked this user
        IF EXISTS (
            SELECT 1 FROM swipes
            WHERE user_id = p_target_user_id
              AND target_user_id = p_user_id
              AND action IN ('LIKE', 'SUPER_LIKE')
        ) THEN
            -- Determine user order for match table
            IF p_user_id < p_target_user_id THEN
                v_user1 := p_user_id;
                v_user2 := p_target_user_id;
            ELSE
                v_user1 := p_target_user_id;
                v_user2 := p_user_id;
            END IF;

            -- Create match (ignore if exists)
            INSERT INTO matches (user1_id, user2_id)
            VALUES (v_user1, v_user2)
            ON CONFLICT (user1_id, user2_id) DO NOTHING
            RETURNING id INTO v_match_id;

            IF v_match_id IS NOT NULL THEN
                v_is_match := TRUE;

                -- Create notifications for both users
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

-- ========================================
-- FUNCTION: Get Feed for User
-- Optimized feed generation
-- ========================================
CREATE OR REPLACE FUNCTION get_user_feed(
    p_user_id UUID,
    p_limit INT DEFAULT 20,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    user_id UUID,
    username VARCHAR,
    first_name VARCHAR,
    age INT,
    gender VARCHAR,
    bio TEXT,
    profile_picture_url VARCHAR,
    is_verified BOOLEAN,
    compatibility_score NUMERIC
) AS $$
DECLARE
    v_user_prefs RECORD;
BEGIN
    -- Get user preferences
    SELECT
        min_age, max_age, interested_in
    INTO v_user_prefs
    FROM user_preferences
    WHERE user_preferences.user_id = p_user_id;

    RETURN QUERY
    WITH excluded_users AS (
        -- Users already swiped
        SELECT target_user_id FROM swipes WHERE swipes.user_id = p_user_id
        UNION
        -- Blocked users (both directions)
        SELECT blocked_id FROM user_blocks WHERE blocker_id = p_user_id
        UNION
        SELECT blocker_id FROM user_blocks WHERE blocked_id = p_user_id
        UNION
        -- Already matched
        SELECT CASE WHEN user1_id = p_user_id THEN user2_id ELSE user1_id END
        FROM matches WHERE user1_id = p_user_id OR user2_id = p_user_id
    )
    SELECT
        fc.id,
        fc.username,
        fc.first_name,
        fc.age,
        fc.gender,
        fc.bio,
        fc.profile_picture_url,
        fc.is_verified,
        COALESCE(r.score, 50.0) AS compatibility_score
    FROM feed_candidates fc
    LEFT JOIN recommendations r ON r.user_id = p_user_id AND r.target_user_id = fc.id
    WHERE fc.id != p_user_id
      AND fc.id NOT IN (SELECT * FROM excluded_users)
      -- FIX: Handle NULL preferences for new users (return all candidates in age range)
      AND (v_user_prefs IS NULL OR fc.age BETWEEN COALESCE(v_user_prefs.min_age, 18) AND COALESCE(v_user_prefs.max_age, 99))
      AND (
          v_user_prefs IS NULL
          OR v_user_prefs.interested_in IS NULL
          OR v_user_prefs.interested_in IN ('BOTH', 'EVERYONE')
          OR fc.gender = v_user_prefs.interested_in
      )
    ORDER BY
        COALESCE(r.score, 50.0) DESC,
        fc.last_active DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_user_feed(UUID, INT, INT) IS 'Get feed candidates for user with scoring';

-- ========================================
-- FUNCTION: Get User Matches
-- With conversation info
-- ========================================
CREATE OR REPLACE FUNCTION get_user_matches(
    p_user_id UUID,
    p_limit INT DEFAULT 20,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    match_id UUID,
    matched_user_id UUID,
    matched_username VARCHAR,
    matched_name VARCHAR,
    profile_picture_url VARCHAR,
    matched_at TIMESTAMP,
    last_message TEXT,
    last_message_time TIMESTAMP,
    unread_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        m.id,
        CASE WHEN m.user1_id = p_user_id THEN m.user2_id ELSE m.user1_id END,
        CASE WHEN m.user1_id = p_user_id THEN u2.username ELSE u1.username END,
        CASE WHEN m.user1_id = p_user_id THEN u2.first_name ELSE u1.first_name END,
        CASE WHEN m.user1_id = p_user_id THEN u2.profile_picture_url ELSE u1.profile_picture_url END,
        m.matched_at,
        (
            SELECT content
            FROM messages msg
            WHERE msg.match_id = m.id
            ORDER BY msg.created_at DESC
            LIMIT 1
        ),
        (
            SELECT created_at
            FROM messages msg
            WHERE msg.match_id = m.id
            ORDER BY msg.created_at DESC
            LIMIT 1
        ),
        (
            SELECT COUNT(*)
            FROM messages msg
            WHERE msg.match_id = m.id
              AND msg.sender_id != p_user_id
              AND msg.status != 'READ'
        )
    FROM matches m
    JOIN users u1 ON u1.id = m.user1_id
    JOIN users u2 ON u2.id = m.user2_id
    WHERE m.status = 'ACTIVE'
      AND (m.user1_id = p_user_id OR m.user2_id = p_user_id)
    ORDER BY
        COALESCE(
            (SELECT MAX(created_at) FROM messages msg WHERE msg.match_id = m.id),
            m.matched_at
        ) DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_user_matches(UUID, INT, INT) IS 'Get user matches with conversation info';

-- ========================================
-- FUNCTION: Calculate Compatibility Score
-- Simple scoring algorithm
-- ========================================
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

    -- Age compatibility (max 20 points)
    SELECT ABS(
        calculate_age((SELECT date_of_birth FROM users WHERE id = p_user1_id)) -
        calculate_age((SELECT date_of_birth FROM users WHERE id = p_user2_id))
    ) INTO v_age_diff;

    IF v_age_diff <= 5 THEN
        v_score := v_score + 20;
    ELSIF v_age_diff <= 10 THEN
        v_score := v_score + 10;
    END IF;

    RETURN LEAST(v_score, 100.0);
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION calculate_compatibility(UUID, UUID) IS 'Calculate compatibility score between two users';

-- ========================================
-- FUNCTION: Cleanup Old Data
-- Remove expired and old records
-- ========================================
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
    -- Delete old PASS swipes (keep LIKE and SUPER_LIKE)
    WITH deleted AS (
        DELETE FROM swipes
        WHERE action = 'PASS'
          AND created_at < NOW() - (p_days_swipes || ' days')::INTERVAL
        RETURNING 1
    )
    SELECT COUNT(*) INTO v_swipes FROM deleted;

    -- Delete expired recommendations
    WITH deleted AS (
        DELETE FROM recommendations
        WHERE expires_at < NOW()
           OR created_at < NOW() - (p_days_recommendations || ' days')::INTERVAL
        RETURNING 1
    )
    SELECT COUNT(*) INTO v_recommendations FROM deleted;

    -- Delete expired/revoked tokens
    WITH deleted AS (
        DELETE FROM refresh_tokens
        WHERE expires_at < NOW()
           OR revoked = true
        RETURNING 1
    )
    SELECT COUNT(*) INTO v_tokens FROM deleted;

    -- Delete used/expired verification codes
    WITH deleted AS (
        DELETE FROM verification_codes
        WHERE expires_at < NOW()
           OR used_at IS NOT NULL
        RETURNING 1
    )
    SELECT COUNT(*) INTO v_codes FROM deleted;

    RETURN QUERY SELECT v_swipes, v_recommendations, v_tokens, v_codes;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_data(INT, INT, INT) IS 'Clean up expired and old data';

-- ========================================
-- FUNCTION: Get Database Stats
-- For monitoring
-- ========================================
CREATE OR REPLACE FUNCTION get_database_stats()
RETURNS TABLE (
    table_name TEXT,
    row_count BIGINT,
    total_size TEXT,
    index_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        relname::TEXT,
        reltuples::BIGINT,
        pg_size_pretty(pg_total_relation_size(oid)),
        pg_size_pretty(pg_indexes_size(oid))
    FROM pg_class
    WHERE relkind = 'r'
      AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
    ORDER BY pg_total_relation_size(oid) DESC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_database_stats() IS 'Get table sizes and row counts for monitoring';
