-- POC Dating Database Schema
-- Version: 2.0
--
-- PURPOSE: Core table definitions for dating application
--
-- MODULARITY:
-- - 01-schema.sql   (tables + constraints)
-- - 02-indexes.sql  (all indexes)
-- - 03-views.sql    (materialized views)
-- - 04-functions.sql (stored procedures)
-- - 05-seed-data.sql (test data)
--
-- EXECUTION:
-- docker-compose up postgres will auto-run these files in order
-- OR: psql -U dating_user -d dating_db -f 01-schema.sql

-- ========================================
-- EXTENSIONS (Must be first)
-- ========================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- Text search optimization
CREATE EXTENSION IF NOT EXISTS "btree_gin";      -- GIN index for arrays

-- ========================================
-- USERS TABLE
-- Core user profiles and authentication
-- ========================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE,
    gender VARCHAR(20),
    bio TEXT,
    profile_picture_url VARCHAR(500),
    location_lat DECIMAL(10, 8),
    location_lng DECIMAL(11, 8),
    is_verified BOOLEAN DEFAULT false,
    is_premium BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,

    -- Generated column for age (modern PostgreSQL feature)
    age INT GENERATED ALWAYS AS (
        CASE WHEN date_of_birth IS NOT NULL
        THEN EXTRACT(YEAR FROM AGE(date_of_birth))::INT
        ELSE NULL END
    ) STORED,

    CONSTRAINT valid_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED', 'PENDING')),
    CONSTRAINT valid_gender CHECK (gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'OTHER'))
);

COMMENT ON TABLE users IS 'Core user profiles and authentication';
COMMENT ON COLUMN users.age IS 'Auto-computed age from date_of_birth - indexed for feed filtering';

-- ========================================
-- USER PREFERENCES TABLE
-- Matching preferences and settings
-- ========================================
CREATE TABLE IF NOT EXISTS user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    min_age INT DEFAULT 18 CHECK (min_age >= 18),
    max_age INT DEFAULT 99 CHECK (max_age <= 150),
    max_distance_km INT DEFAULT 50 CHECK (max_distance_km > 0),
    interested_in VARCHAR(20) DEFAULT 'BOTH',
    interests TEXT[],
    show_me_to_others BOOLEAN DEFAULT true,
    notification_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_age_range CHECK (min_age <= max_age),
    CONSTRAINT valid_interested_in CHECK (interested_in IN ('MALE', 'FEMALE', 'BOTH', 'EVERYONE'))
);

COMMENT ON TABLE user_preferences IS 'User matching preferences and settings';

-- ========================================
-- PHOTOS TABLE
-- User photo management with ordering
-- ========================================
CREATE TABLE IF NOT EXISTS photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN DEFAULT false,
    is_verified BOOLEAN DEFAULT false,
    moderation_status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_moderation_status CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

COMMENT ON TABLE photos IS 'User photos with display ordering and moderation status';

-- ========================================
-- SWIPES TABLE
-- High-frequency swipe events
-- ========================================
CREATE TABLE IF NOT EXISTS swipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_action CHECK (action IN ('LIKE', 'PASS', 'SUPER_LIKE')),
    CONSTRAINT no_self_swipe CHECK (user_id != target_user_id),
    UNIQUE(user_id, target_user_id)
);

COMMENT ON TABLE swipes IS 'High-frequency swipe events, indexed for feed exclusion';

-- ========================================
-- MATCHES TABLE
-- Mutual matches between users
-- ========================================
CREATE TABLE IF NOT EXISTS matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    ended_by UUID REFERENCES users(id),

    CONSTRAINT no_self_match CHECK (user1_id != user2_id),
    CONSTRAINT user1_before_user2 CHECK (user1_id < user2_id),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'UNMATCHED', 'BLOCKED')),
    UNIQUE(user1_id, user2_id)
);

COMMENT ON TABLE matches IS 'Mutual matches between users with status tracking';

-- ========================================
-- MATCH SCORES TABLE
-- Compatibility scores for matches
-- ========================================
CREATE TABLE IF NOT EXISTS match_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    score NUMERIC(5, 2) CHECK (score >= 0 AND score <= 100),
    factors JSONB,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE match_scores IS 'Compatibility scores with factor breakdown';

-- ========================================
-- MESSAGES TABLE
-- Chat messages with delivery tracking
-- ========================================
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    status VARCHAR(20) DEFAULT 'SENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT valid_status CHECK (status IN ('SENT', 'DELIVERED', 'READ')),
    CONSTRAINT valid_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'GIF', 'AUDIO'))
);

COMMENT ON TABLE messages IS 'Chat messages with delivery and read receipts';

-- ========================================
-- REFRESH TOKENS TABLE
-- JWT token management
-- ========================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address INET,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP
);

COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens with device tracking';

-- ========================================
-- RECOMMENDATIONS TABLE
-- ML/Algorithm-based recommendations
-- ========================================
CREATE TABLE IF NOT EXISTS recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score NUMERIC(5, 2) CHECK (score >= 0 AND score <= 100),
    algorithm_version VARCHAR(20) DEFAULT 'v1',
    factors JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,

    CONSTRAINT no_self_recommendation CHECK (user_id != target_user_id),
    UNIQUE(user_id, target_user_id)
);

COMMENT ON TABLE recommendations IS 'Pre-computed ML recommendations with scores';

-- ========================================
-- USER BLOCKS TABLE
-- User blocking functionality
-- ========================================
CREATE TABLE IF NOT EXISTS user_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT no_self_block CHECK (blocker_id != blocked_id),
    UNIQUE(blocker_id, blocked_id)
);

COMMENT ON TABLE user_blocks IS 'User blocking for safety and privacy';

-- ========================================
-- NOTIFICATIONS TABLE
-- Push and in-app notifications
-- ========================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    data JSONB,
    is_read BOOLEAN DEFAULT false,
    is_sent BOOLEAN DEFAULT false,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_notification_type CHECK (type IN (
        'NEW_MATCH', 'NEW_MESSAGE', 'SUPER_LIKE',
        'PROFILE_VIEW', 'SYSTEM', 'PROMOTION'
    ))
);

COMMENT ON TABLE notifications IS 'Push and in-app notification queue';

-- ========================================
-- VERIFICATION CODES TABLE
-- Email/phone verification
-- ========================================
CREATE TABLE IF NOT EXISTS verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    attempts INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_verification_type CHECK (type IN ('EMAIL', 'PHONE', 'PASSWORD_RESET'))
);

COMMENT ON TABLE verification_codes IS 'Verification codes for email/phone/password reset';

-- ========================================
-- INTERACTION HISTORY TABLE
-- Analytics and behavior tracking
-- ========================================
CREATE TABLE IF NOT EXISTS interaction_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    target_id UUID REFERENCES users(id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE interaction_history IS 'User behavior analytics for recommendations';

-- ========================================
-- REPORTS TABLE
-- User report/flag system
-- ========================================
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_reason CHECK (reason IN (
        'SPAM', 'HARASSMENT', 'INAPPROPRIATE_CONTENT',
        'FAKE_PROFILE', 'SCAM', 'OTHER'
    )),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'REVIEWING', 'RESOLVED', 'DISMISSED'))
);

COMMENT ON TABLE reports IS 'User reports for moderation';

-- ========================================
-- AUDIT LOGS TABLE
-- System audit trail
-- ========================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100),
    entity_id UUID,
    action VARCHAR(50),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    changes JSONB,
    ip_address INET,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit_logs IS 'Audit trail for compliance and debugging';

-- ========================================
-- TRIGGER: Update updated_at timestamp
-- ========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tables with updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_photos_updated_at
    BEFORE UPDATE ON photos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_match_scores_updated_at
    BEFORE UPDATE ON match_scores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
