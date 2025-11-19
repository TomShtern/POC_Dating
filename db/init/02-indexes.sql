-- POC Dating Database Indexes
-- Version: 3.0
--
-- PURPOSE: Optimized indexes for all query patterns
--
-- INDEX STRATEGY:
-- 1. Primary keys auto-indexed
-- 2. Foreign keys indexed for JOINs
-- 3. Composite indexes for common WHERE + ORDER BY
-- 4. Partial indexes for filtered queries
-- 5. GIN indexes for array/JSONB columns
-- 6. Covering indexes for high-frequency queries

-- ========================================
-- USERS TABLE INDEXES
-- High priority: Auth, feed filtering, search
-- ========================================

-- Auth queries (login by email)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Username lookup
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Feed filtering: gender + age range
-- Composite for: WHERE gender = ? AND age BETWEEN ? AND ?
CREATE INDEX IF NOT EXISTS idx_users_gender_age ON users(gender, age);

-- Note: idx_users_gender_dob removed as redundant with idx_users_gender_age

-- Active users filter (partial index for efficiency)
CREATE INDEX IF NOT EXISTS idx_users_active ON users(status, last_active DESC)
    WHERE status = 'ACTIVE';

-- Status lookup for filtering
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Location-based queries (for future geo-features)
CREATE INDEX IF NOT EXISTS idx_users_location ON users(location_lat, location_lng)
    WHERE location_lat IS NOT NULL AND location_lng IS NOT NULL;

-- Premium user queries
CREATE INDEX IF NOT EXISTS idx_users_premium ON users(is_premium)
    WHERE is_premium = true;

-- Active premium users for premium-only features
CREATE INDEX IF NOT EXISTS idx_users_premium_active ON users(id)
    WHERE is_premium = true AND status = 'ACTIVE';

-- Full-text search on bio (trigram for LIKE queries)
CREATE INDEX IF NOT EXISTS idx_users_bio_trgm ON users USING gin(bio gin_trgm_ops)
    WHERE bio IS NOT NULL;

-- ========================================
-- USER PREFERENCES INDEXES
-- ========================================

-- User lookup
CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences(user_id);

-- Interest matching with GIN for array contains
CREATE INDEX IF NOT EXISTS idx_user_preferences_interests ON user_preferences USING gin(interests);

-- ========================================
-- PHOTOS INDEXES
-- ========================================

-- Single-column FK index for basic user lookups
CREATE INDEX IF NOT EXISTS idx_photos_user_id ON photos(user_id);

-- User's photos with ordering
CREATE INDEX IF NOT EXISTS idx_photos_user_order ON photos(user_id, display_order);

-- Primary photo lookup
CREATE INDEX IF NOT EXISTS idx_photos_primary ON photos(user_id, is_primary)
    WHERE is_primary = true;

-- Moderation queue
CREATE INDEX IF NOT EXISTS idx_photos_moderation ON photos(moderation_status, created_at)
    WHERE moderation_status = 'PENDING';

-- Ensure only one primary photo per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_photos_one_primary ON photos(user_id)
    WHERE is_primary = true;

-- ========================================
-- SWIPES INDEXES (Critical for performance)
-- ========================================

-- Swipe lookup (duplicate check)
-- Covers: SELECT * FROM swipes WHERE user_id = ? AND target_user_id = ?
CREATE INDEX IF NOT EXISTS idx_swipes_user_target ON swipes(user_id, target_user_id);

-- Daily swipe count query
-- Covers: SELECT COUNT(*) FROM swipes WHERE user_id = ? AND created_at > ?
CREATE INDEX IF NOT EXISTS idx_swipes_user_time ON swipes(user_id, created_at DESC);

-- Feed exclusion (already swiped users)
-- Covers: WHERE target_user_id NOT IN (SELECT target_user_id FROM swipes WHERE user_id = ?)
CREATE INDEX IF NOT EXISTS idx_swipes_target_user ON swipes(target_user_id);

-- Like queries for match detection
CREATE INDEX IF NOT EXISTS idx_swipes_likes ON swipes(target_user_id, user_id, action)
    WHERE action IN ('LIKE', 'SUPER_LIKE');

-- ========================================
-- MATCHES INDEXES
-- ========================================

-- User's matches (either side)
CREATE INDEX IF NOT EXISTS idx_matches_user1 ON matches(user1_id);
CREATE INDEX IF NOT EXISTS idx_matches_user2 ON matches(user2_id);

-- Active matches only (partial index)
-- Covers: WHERE (user1_id = ? OR user2_id = ?) AND status = 'ACTIVE'
CREATE INDEX IF NOT EXISTS idx_matches_active_user1 ON matches(user1_id, matched_at DESC)
    WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_matches_active_user2 ON matches(user2_id, matched_at DESC)
    WHERE status = 'ACTIVE';

-- Recent matches
CREATE INDEX IF NOT EXISTS idx_matches_time ON matches(matched_at DESC);

-- Ended by user lookup (for analytics and moderation)
CREATE INDEX IF NOT EXISTS idx_matches_ended_by ON matches(ended_by)
    WHERE ended_by IS NOT NULL;

-- Note: idx_matches_status removed as redundant with idx_matches_active_user1/user2

-- ========================================
-- MATCH SCORES INDEXES
-- ========================================

CREATE INDEX IF NOT EXISTS idx_match_scores_match ON match_scores(match_id);
CREATE INDEX IF NOT EXISTS idx_match_scores_score ON match_scores(score DESC);

-- ========================================
-- MESSAGES INDEXES (Chat performance)
-- ========================================

-- Chat history (most critical)
-- Covers: SELECT * FROM messages WHERE match_id = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_messages_match_time ON messages(match_id, created_at DESC);

-- Covering index for message queries (avoids heap lookups)
CREATE INDEX IF NOT EXISTS idx_messages_match_covering ON messages(match_id, created_at DESC)
    INCLUDE (sender_id, content, status);

-- Sender's messages
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);

-- Unread messages count (includes sender_id for efficient unread-by-user queries)
CREATE INDEX IF NOT EXISTS idx_messages_unread ON messages(match_id, sender_id, status)
    WHERE status != 'READ' AND deleted_at IS NULL;

-- Note: idx_messages_recent removed as it's redundant with idx_messages_match_time

-- ========================================
-- REFRESH TOKENS INDEXES
-- ========================================

-- Token lookup
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

-- Cleanup expired tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at)
    WHERE revoked = false;

-- Valid tokens only
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_valid ON refresh_tokens(token_hash)
    WHERE revoked = false;

-- ========================================
-- RECOMMENDATIONS INDEXES (Feed generation)
-- ========================================

-- Target user lookup for reverse queries
CREATE INDEX IF NOT EXISTS idx_recommendations_target ON recommendations(target_user_id);

-- Expired recommendations cleanup
CREATE INDEX IF NOT EXISTS idx_recommendations_expires ON recommendations(expires_at)
    WHERE expires_at IS NOT NULL;

-- Active recommendations only (consolidated - covers user_id + score queries)
-- Note: idx_recommendations_user_score removed as redundant
CREATE INDEX IF NOT EXISTS idx_recommendations_active ON recommendations(user_id, score DESC)
    WHERE expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP;

-- ========================================
-- USER BLOCKS INDEXES
-- ========================================

-- Blocker's list
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocker ON user_blocks(blocker_id);

-- Blocked user check (bi-directional)
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked ON user_blocks(blocked_id);

-- Combined for quick block check
CREATE INDEX IF NOT EXISTS idx_user_blocks_pair ON user_blocks(blocker_id, blocked_id);

-- ========================================
-- NOTIFICATIONS INDEXES
-- ========================================

-- User's notifications
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, created_at DESC);

-- Unread notifications (optimized partial index)
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id, is_read)
    WHERE is_read = false;

-- Unsent notifications (push queue)
CREATE INDEX IF NOT EXISTS idx_notifications_unsent ON notifications(is_sent, created_at)
    WHERE is_sent = false;

-- ========================================
-- VERIFICATION CODES INDEXES
-- ========================================

-- User's verification codes
CREATE INDEX IF NOT EXISTS idx_verification_user ON verification_codes(user_id);

-- Active codes only (optimized for lookup)
CREATE INDEX IF NOT EXISTS idx_verification_active ON verification_codes(user_id, type, expires_at)
    WHERE used_at IS NULL;

-- Cleanup expired codes
CREATE INDEX IF NOT EXISTS idx_verification_expires ON verification_codes(expires_at)
    WHERE used_at IS NULL;

-- ========================================
-- INTERACTION HISTORY INDEXES
-- ========================================

-- User history
CREATE INDEX IF NOT EXISTS idx_interaction_user ON interaction_history(user_id, created_at DESC);

-- Target user lookup for reverse queries
CREATE INDEX IF NOT EXISTS idx_interaction_history_target ON interaction_history(target_id);

-- Action type analytics
CREATE INDEX IF NOT EXISTS idx_interaction_action ON interaction_history(action, created_at DESC);

-- ========================================
-- REPORTS INDEXES
-- ========================================

-- Moderation queue
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status, created_at)
    WHERE status IN ('PENDING', 'REVIEWING');

-- User's reports
CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_reports_reported ON reports(reported_user_id);

-- Resolved by moderator lookup
CREATE INDEX IF NOT EXISTS idx_reports_resolved_by ON reports(resolved_by)
    WHERE resolved_by IS NOT NULL;

-- ========================================
-- AUDIT LOGS INDEXES
-- ========================================

-- Entity lookup
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- Time-based queries
CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(created_at DESC);

-- User actions
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id, created_at DESC)
    WHERE user_id IS NOT NULL;

-- ========================================
-- STATISTICS UPDATE
-- Run ANALYZE after creating indexes
-- ========================================
ANALYZE users;
ANALYZE user_preferences;
ANALYZE photos;
ANALYZE swipes;
ANALYZE matches;
ANALYZE match_scores;
ANALYZE messages;
ANALYZE refresh_tokens;
ANALYZE recommendations;
ANALYZE user_blocks;
ANALYZE notifications;
ANALYZE verification_codes;
ANALYZE interaction_history;
ANALYZE reports;
ANALYZE audit_logs;
