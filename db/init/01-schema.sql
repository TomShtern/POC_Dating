-- POC Dating Database Schema
--
-- PURPOSE: Initialize PostgreSQL database with all tables
--
-- WHY SQL SCRIPTS:
-- - Version control friendly
-- - Clear schema documentation
-- - Can be version controlled and reviewed
-- - Standard SQL works across databases
--
-- ACTUAL IMPLEMENTATION:
-- In production, use Flyway or Liquibase for migrations
-- This is a simple initialization script for POC
--
-- EXECUTION:
-- docker-compose up postgres will auto-run this file
-- OR: psql -U dating_user -d dating_db -f 01-schema.sql

-- ========================================
-- USERS TABLE
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
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, DELETED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    CONSTRAINT valid_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- ========================================
-- USER PREFERENCES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    min_age INT DEFAULT 18 CHECK (min_age >= 18),
    max_age INT DEFAULT 99 CHECK (max_age <= 150),
    max_distance_km INT DEFAULT 50 CHECK (max_distance_km > 0),
    interested_in VARCHAR(20) DEFAULT 'BOTH', -- MALE, FEMALE, BOTH
    interests TEXT[], -- Array of interest tags (JSON in text format)
    notification_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_age_range CHECK (min_age <= max_age),
    CONSTRAINT valid_interested_in CHECK (interested_in IN ('MALE', 'FEMALE', 'BOTH'))
);

CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);

-- ========================================
-- SWIPES TABLE (High-frequency data)
-- ========================================
CREATE TABLE IF NOT EXISTS swipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL, -- LIKE, PASS, SUPER_LIKE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_action CHECK (action IN ('LIKE', 'PASS', 'SUPER_LIKE')),
    CONSTRAINT no_self_swipe CHECK (user_id != target_user_id),
    UNIQUE(user_id, target_user_id) -- Prevent duplicate swipes
);

CREATE INDEX idx_swipes_user_id ON swipes(user_id);
CREATE INDEX idx_swipes_target_user_id ON swipes(target_user_id);
CREATE INDEX idx_swipes_created_at ON swipes(created_at DESC);
CREATE INDEX idx_swipes_user_created ON swipes(user_id, created_at DESC);

-- ========================================
-- MATCHES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    CONSTRAINT no_self_match CHECK (user1_id != user2_id),
    CONSTRAINT user1_before_user2 CHECK (user1_id < user2_id), -- Ensure consistent ordering
    UNIQUE(user1_id, user2_id)
);

CREATE INDEX idx_matches_user1_id ON matches(user1_id);
CREATE INDEX idx_matches_user2_id ON matches(user2_id);
CREATE INDEX idx_matches_matched_at ON matches(matched_at DESC);

-- ========================================
-- MATCH SCORES TABLE (For recommendations)
-- ========================================
CREATE TABLE IF NOT EXISTS match_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    score NUMERIC(5, 2) CHECK (score >= 0 AND score <= 100),
    factors JSONB, -- {interest_match: 40, age_match: 30, preferences: 30}
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_scores_match_id ON match_scores(match_id);
CREATE INDEX idx_match_scores_score ON match_scores(score DESC);

-- ========================================
-- MESSAGES TABLE (Chat messages)
-- ========================================
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'SENT', -- SENT, DELIVERED, READ
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT valid_status CHECK (status IN ('SENT', 'DELIVERED', 'READ'))
);

CREATE INDEX idx_messages_match_id ON messages(match_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_match_created ON messages(match_id, created_at DESC);
CREATE INDEX idx_messages_status ON messages(status) WHERE deleted_at IS NULL;

-- ========================================
-- REFRESH TOKENS TABLE (JWT management)
-- ========================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked) WHERE revoked = false;

-- ========================================
-- RECOMMENDATIONS TABLE (For ML/algorithms)
-- ========================================
CREATE TABLE IF NOT EXISTS recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recommended_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score NUMERIC(5, 2) CHECK (score >= 0 AND score <= 100),
    algorithm_version VARCHAR(20),
    factors JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT no_self_recommendation CHECK (user_id != recommended_user_id)
);

CREATE INDEX idx_recommendations_user_id ON recommendations(user_id);
CREATE INDEX idx_recommendations_created_at ON recommendations(created_at DESC);
CREATE INDEX idx_recommendations_expires_at ON recommendations(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_recommendations_score ON recommendations(score DESC);

-- ========================================
-- INTERACTION HISTORY TABLE (For analytics)
-- ========================================
CREATE TABLE IF NOT EXISTS interaction_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL, -- view, like, pass, match, message, etc
    target_id UUID REFERENCES users(id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interaction_history_user_id ON interaction_history(user_id);
CREATE INDEX idx_interaction_history_action ON interaction_history(action);
CREATE INDEX idx_interaction_history_created_at ON interaction_history(created_at DESC);

-- ========================================
-- AUDIT/LOGS (Optional but recommended)
-- ========================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100),
    entity_id UUID,
    action VARCHAR(50), -- CREATE, UPDATE, DELETE
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    changes JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- ========================================
-- EXTENSIONS
-- ========================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Text search optimization

-- ========================================
-- COMMENTS
-- ========================================
COMMENT ON TABLE users IS 'Core user profiles and authentication';
COMMENT ON TABLE user_preferences IS 'User matching preferences and settings';
COMMENT ON TABLE swipes IS 'High-frequency swipe events, indexed for queries';
COMMENT ON TABLE matches IS 'Mutual matches between users';
COMMENT ON TABLE messages IS 'Chat messages with delivery status tracking';
COMMENT ON TABLE refresh_tokens IS 'JWT token management for auth';
COMMENT ON TABLE recommendations IS 'ML/Algorithm-based recommendations cache';
COMMENT ON TABLE interaction_history IS 'Analytics data for user behavior';

PRINT 'Database schema initialized successfully!';
